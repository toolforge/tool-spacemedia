package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkId;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Duplicate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MetadataProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.service.commons.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import io.micrometer.core.instrument.util.StringUtils;

@Service
public class MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private static final List<String> STRINGS_TO_REMOVE = Arrays.asList(" rel=\"noreferrer nofollow\"");

    private static final List<String> STRINGS_TO_REPLACE_BY_SPACE = Arrays.asList("&nbsp;", "  ");

    // Taken from https://github.com/eatcha-wikimedia/YouTubeReviewBot/blob/master/main.py
    private static final Pattern FROM_YOUTUBE = Pattern.compile(
            ".*\\{\\{\\s*?[Ff]rom\\s[Yy]ou[Tt]ube\\s*(?:\\||\\|1\\=|\\s*?)(?:\\s*)(?:1|=\\||)(?:=|)([^\"&?\\/ \\}]{11}).*",
            Pattern.DOTALL);

    // Taken from https://github.com/eatcha-wikimedia/YouTubeReviewBot/blob/master/main.py
    private static final Pattern YOUTUBE_URL = Pattern.compile(
            ".*https?\\:\\/\\/(?:www|m|)(?:|\\.)youtube\\.com/watch\\W(?:feature\\=player_embedded&)?v\\=([^\"&?\\/ \\}]{11}).*",
            Pattern.DOTALL);

    @Autowired
    private MediaService self;

    @Autowired
    private CommonsService commonsService;

    @Autowired
    private YouTubeVideoRepository youtubeRepository;

    @Autowired
    private HashAssociationRepository hashRepository;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${perceptual.threshold.variant}")
    private double perceptualThresholdVariant;

    @Value("${update.fullres.images}")
    private boolean updateFullResImages;

    private Set<String> blockListIgnoredTerms;

    @PostConstruct
    public void init() throws IOException {
        blockListIgnoredTerms = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.terms.csv"));
    }

    public MediaUpdateResult updateMedia(Media<?, ?> media, MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo,
            boolean forceUpdate) throws IOException {
        return updateMedia(media, originalRepo, forceUpdate, null);
    }

    public MediaUpdateResult updateMedia(Media<?, ?> media, MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo,
            boolean forceUpdate, Path localPath) throws IOException {
        boolean result = false;
        if (cleanupDescription(media)) {
            result = true;
        }
        MediaUpdateResult ur = updateReadableStateAndHashes(media, localPath, forceUpdate);
        if (ur.getResult()) {
            result = true;
        }
        if (findCommonsFilesWithSha1(media) || findCommonsFilesWithPhash(media)) {
            result = true;
        }
        if (originalRepo != null && findDuplicatesInRepository(media, originalRepo)) {
            result = true;
        }
        if (!Boolean.TRUE.equals(media.isIgnored()) && belongsToBlocklist(media)) {
            result = true;
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    private boolean belongsToBlocklist(Media<?, ?> media) {
        String titleAndDescription = "";
        if (media.getTitle() != null) {
            titleAndDescription += media.getTitle().toLowerCase(Locale.ENGLISH);
        }
        if (media.getDescription() != null) {
            titleAndDescription += media.getDescription().toLowerCase(Locale.ENGLISH);
        }
        if (!titleAndDescription.isEmpty()) {
            String ignoredTerms = blockListIgnoredTerms.parallelStream().filter(titleAndDescription::contains).sorted()
                    .collect(joining(","));
            if (!ignoredTerms.isEmpty()) {
                media.setIgnoredReason("Title or description contains term(s) in block list: " + ignoredTerms);
                media.setIgnored(Boolean.TRUE);
                return true;
            }
        }
        return false;
    }

    public boolean findDuplicatesInRepository(Media<?, ?> media, MediaRepository<? extends Media<?, ?>, ?, ?> repo) {
        boolean result = false;
        // Find exact duplicates by SHA-1
        String sha1 = media.getMetadata().getSha1();
        if (sha1 != null && handleExactDuplicates(media,
                repo instanceof FullResMediaRepository<?, ?, ?>
                        ? ((FullResMediaRepository<?, ?, ?>) repo).findByMetadata_Sha1OrFullResMetadata_Sha1(sha1)
                        : repo.findByMetadata_Sha1(sha1))) {
            result = true;
        }
        // Find exact duplicates by perceptual hash
        String phash = media.getMetadata().getPhash();
        if (phash != null && handleExactDuplicates(media,
                repo instanceof FullResMediaRepository<?, ?, ?>
                        ? ((FullResMediaRepository<?, ?, ?>) repo).findByMetadata_PhashOrFullResMetadata_Phash(phash)
                        : repo.findByMetadata_Phash(phash))) {
            result = true;
        }
        // Find almost duplicates by perceptual hash
        BigInteger perceptualHash = media.getMetadata().getPerceptualHash();
        if (perceptualHash != null && handleDuplicatesAndVariants(media, repo
                .findByMetadata_PhashNotNull()
                .parallelStream()
                .filter(m -> !m.getId().equals(media.getId()))
                .map(m -> new DuplicateHolder(m, HashHelper.similarityScore(perceptualHash, m.getMetadata().getPhash())))
                .filter(h -> h.similarityScore < perceptualThreshold)
                .collect(toSet()))) {
            result = true;
        }
        return result;
    }

    private static class DuplicateHolder {
        private final MediaProjection<?> media;
        private final double similarityScore;

        DuplicateHolder(MediaProjection<?> media, double similarityScore) {
            this.media = media;
            this.similarityScore = similarityScore;
        }

        Duplicate toDuplicate() {
            return new Duplicate(media.getId().toString(), similarityScore);
        }

        @Override
        public int hashCode() {
            return Objects.hash(media.getId());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            return Objects.equals(media.getId(), ((DuplicateHolder) obj).media.getId());
        }
    }

    private static class DuplicateIdMediaProjection implements MediaProjection<Object> {

        private final Object id;

        DuplicateIdMediaProjection(Object id) {
            this.id = id;
        }

        @Override
        public Object getId() {
            return id;
        }

        @Override
        public MetadataProjection getMetadata() {
            return null;
        }

        @Override
        public Set<Duplicate> getDuplicates() {
            return Collections.emptySet();
        }

        @Override
        public Set<Duplicate> getVariants() {
            return Collections.emptySet();
        }
    }

    private boolean handleExactDuplicates(Media<?, ?> media, List<? extends Media<?, ?>> exactDuplicates) {
        return handleDuplicatesAndVariants(media, exactDuplicates.stream()
                .filter(x -> !Objects.equals(x.getId(), media.getId()))
                .map(d -> new DuplicateHolder(new DuplicateIdMediaProjection(d.getId()), 0d))
                .collect(toSet()));
    }

    /**
     * Handles duplicates and variants for a given media.
     *
     * @param media The currently considered media
     * @param duplicateHolders set of identified duplicates and variants, with their similiarity score
     * @return {@code true} if media has been updated and must be persisted
     */
    private boolean handleDuplicatesAndVariants(Media<?, ?> media, Set<DuplicateHolder> duplicateHolders) {
        boolean result = false;
        // Duplicates are media with a low similarity score. Usually exact duplicates with small variations in resolution
        Set<Duplicate> duplicates = duplicateHolders.stream()
                .filter(h -> !media.considerVariants() || h.similarityScore < perceptualThresholdVariant)
                .map(DuplicateHolder::toDuplicate).collect(toSet());
        if (isNewDuplicateOrVariant(media, duplicateHolders, duplicates, MediaProjection::getDuplicates)) {
            if (CollectionUtils.isEmpty(media.getAllCommonsFileNames())) {
                media.setIgnored(true);
                media.setIgnoredReason("Already present in main repository");
            }
            duplicates.forEach(media::addDuplicate);
            result = true;
        }
        // Variants are media with an higher similarity score. Usually same shapes with different colors
        Set<Duplicate> variants = duplicateHolders.stream()
                .filter(h -> media.considerVariants() && h.similarityScore >= perceptualThresholdVariant)
                .map(DuplicateHolder::toDuplicate).collect(toSet());
        if (isNewDuplicateOrVariant(media, duplicateHolders, variants, MediaProjection::getVariants)) {
            variants.forEach(media::addVariant);
            result = true;
        }
        return result;
    }

    /**
     * Determines if we just found a new duplicate or variant.
     *
     * @param media The currently considered media
     * @param duplicateHolders set of identified duplicates and variants, with their similiarity score
     * @param set set or duplicates or variants
     * @param getter getter method to retrieve duplicates or variants of a given media
     * @return {@code true} if we just found a new duplicate or variant
     */
    private static boolean isNewDuplicateOrVariant(Media<?, ?> media, Set<DuplicateHolder> duplicateHolders,
            Set<Duplicate> set, Function<MediaProjection<?>, Set<Duplicate>> getter) {
        return isNotEmpty(set)
                // Only consider media that are not already known duplicates/variants of the given set
                && (isEmpty(getter.apply(media)) || !getter.apply(media).containsAll(set))
                // Only consider media that are not already known originals for the given set of duplicates/variants
                && duplicateHolders.stream().noneMatch(h -> getter.apply(h.media).stream().anyMatch(
                        d -> d.getOriginalId().equals(media.getId())));
    }

    public MediaUpdateResult updateReadableStateAndHashes(Media<?, ?> media, Path localPath, boolean forceUpdateOfHashes) {
        MediaUpdateResult ur = updateReadableStateAndHashes(media, media.getMetadata(), localPath, forceUpdateOfHashes);
        boolean result = ur.getResult();
        // T230284 - Processing full-res images can lead to OOM errors
        if (updateFullResImages && media instanceof FullResMedia<?, ?>) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            if (updateReadableStateAndHashes(frMedia, frMedia.getFullResMetadata(), localPath, forceUpdateOfHashes)
                    .getResult()) {
                result = true;
            }
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    public static class MediaUpdateResult {
        private final boolean result;
        private final Exception exception;

        public MediaUpdateResult(boolean result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public boolean getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }

    private MediaUpdateResult updateReadableStateAndHashes(Media<?, ?> media, Metadata metadata, Path localPath,
            boolean forceUpdateOfHashes) {
        boolean isImage = media.isImage();
        boolean result = false;
        BufferedImage bi = null;
        try {
            URL assetUrl = metadata.getAssetUrl();
            if (isImage && shouldReadImage(assetUrl, metadata, forceUpdateOfHashes)) {
                try {
                    bi = Utils.readImage(assetUrl, false, true);
                    if (bi != null && !Boolean.TRUE.equals(metadata.isReadableImage())) {
                        metadata.setReadableImage(Boolean.TRUE);
                        result = true;
                    }
                } catch (IOException | URISyntaxException | ImageDecodingException e) {
                    result = ignoreMedia(media, "Unreadable media", e);
                    metadata.setReadableImage(Boolean.FALSE);
                }
            }
            if (isImage && Boolean.TRUE.equals(metadata.isReadableImage())
                    && updatePerceptualHash(metadata, bi, forceUpdateOfHashes)) {
                result = true;
            }
            if (bi != null) {
                bi.flush();
                bi = null;
            }
            if (updateSha1(metadata, localPath, forceUpdateOfHashes)) {
                result = true;
            }
        } catch (RestClientException e) {
            LOGGER.error("Error while computing hashes for {}: {}", media, e.getMessage());
            return new MediaUpdateResult(result, e);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error while computing hashes for " + media, e);
            return new MediaUpdateResult(result, e);
        } finally {
            if (bi != null) {
                bi.flush();
            }
        }
        return new MediaUpdateResult(result, null);
    }

    private static boolean shouldReadImage(URL assetUrl, Metadata metadata, boolean forceUpdateOfHashes) {
        return assetUrl != null
                && (metadata.isReadableImage() == null || (Boolean.TRUE.equals(metadata.isReadableImage())
                        && (metadata.getPhash() == null || metadata.getSha1() == null || forceUpdateOfHashes)));
    }

    private static boolean ignoreMedia(Media<?, ?> media, String reason, Exception e) {
        LOGGER.warn(media.toString(), e);
        media.setIgnored(Boolean.TRUE);
        media.setIgnoredReason(reason + ": " + e.getMessage());
        return true;
    }

    public boolean cleanupDescription(Media<?, ?> media) {
        boolean result = false;
        if (media.getDescription() != null) {
            for (String toRemove : STRINGS_TO_REMOVE) {
                if (media.getDescription().contains(toRemove)) {
                    media.setDescription(media.getDescription().replace(toRemove, ""));
                    result = true;
                }
            }
            for (String toReplace : STRINGS_TO_REPLACE_BY_SPACE) {
                while (media.getDescription().contains(toReplace)) {
                    media.setDescription(media.getDescription().replace(toReplace, " "));
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Computes the media SHA-1.
     *
     * @param metadata media object metadata
     * @param localPath if set, use it instead of asset URL
     * @param forceUpdate {@code true} to force update of an existing hash
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     * @throws URISyntaxException if URL cannot be converted to URI
     */
    public boolean updateSha1(Metadata metadata, Path localPath, boolean forceUpdate)
            throws IOException, URISyntaxException {
        if ((metadata.getSha1() == null || forceUpdate) && (metadata.getAssetUrl() != null || localPath != null)) {
            metadata.setSha1(getSha1(localPath, metadata.getAssetUrl()));
            updateHashes(metadata.getSha1(), metadata.getPhash());
            return true;
        }
        return false;
    }

    private static String getSha1(Path localPath, URL url) throws IOException, URISyntaxException {
        if (localPath != null) {
            return HashHelper.computeSha1(localPath);
        } else {
            return HashHelper.computeSha1(url);
        }
    }

    private void updateHashes(String sha1, String phash) {
        if (sha1 != null) {
            String sha1base36 = CommonsService.base36Sha1(sha1);
            if (!hashRepository.existsById(sha1base36)) {
                hashRepository.save(new HashAssociation(sha1base36, phash));
            }
        }
    }

    /**
     * Computes the perceptual hash of an image, if required.
     *
     * @param metadata  image media metadata
     * @param image     {@code BufferedImage} of asset, can be null if not computed
     * @param forceUpdate {@code true} to force update of an existing hash
     * @return {@code true} if media has been updated with computed perceptual hash
     *         and must be persisted
     */
    public boolean updatePerceptualHash(Metadata metadata, BufferedImage image, boolean forceUpdate) {
        if (image != null && metadata.getAssetUrl() != null && (metadata.getPhash() == null || forceUpdate)) {
            try {
                metadata.setPhash(HashHelper.encode(HashHelper.computePerceptualHash(image)));
            } catch (RuntimeException e) {
                LOGGER.error("Failed to update perceptual hash for {}", metadata, e);
            }
            updateHashes(metadata.getSha1(), metadata.getPhash());
            return true;
        }
        return false;
    }

    /**
     * Looks for Wikimedia Commons files matching the media SHA-1, if required.
     *
     * @param media media object
     * @return {@code true} if media has been updated with list of Wikimedia Commons
     *         files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithSha1(Media<?, ?> media) throws IOException {
        boolean result = false;
        String sha1 = media.getMetadata().getSha1();
        if (sha1 != null && isEmpty(media.getCommonsFileNames())) {
            Set<String> files = commonsService.findFilesWithSha1(sha1);
            if (!files.isEmpty()) {
                media.setCommonsFileNames(files);
                result = true;
            }
        }
        if (media instanceof FullResMedia) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            String fullResSha1 = frMedia.getFullResMetadata().getSha1();
            if (fullResSha1 != null && isEmpty(frMedia.getFullResCommonsFileNames())) {
                Set<String> files = commonsService.findFilesWithSha1(fullResSha1);
                if (!files.isEmpty()) {
                    frMedia.setFullResCommonsFileNames(files);
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Looks for Wikimedia Commons files matching exactly the media perceptual hash,
     * if required.
     *
     * @param media media object
     * @return {@code true} if media has been updated with list of Wikimedia Commons
     *         files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithPhash(Media<?, ?> media) throws IOException {
        boolean result = false;
        String phash = media.getMetadata().getPhash();
        if (phash != null && isEmpty(media.getCommonsFileNames())) {
            List<String> sha1s = hashRepository.findSha1ByPhash(phash);
            if (!sha1s.isEmpty()) {
                Set<String> files = commonsService.findFilesWithSha1(sha1s);
                if (!files.isEmpty()) {
                    media.setCommonsFileNames(files);
                    result = true;
                }
            }
        }
        if (media instanceof FullResMedia) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            String fullResPhash = frMedia.getFullResMetadata().getPhash();
            if (fullResPhash != null && isEmpty(frMedia.getFullResCommonsFileNames())) {
                List<String> fullResSha1s = hashRepository.findSha1ByPhash(fullResPhash);
                if (!fullResSha1s.isEmpty()) {
                    Set<String> files = commonsService.findFilesWithSha1(fullResSha1s);
                    if (!files.isEmpty()) {
                        frMedia.setFullResCommonsFileNames(files);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    private static String findYouTubeId(String text) {
        if (StringUtils.isNotBlank(text)) {
            Matcher m = FROM_YOUTUBE.matcher(text);
            if (m.matches()) {
                return m.group(1).strip();
            } else {
                m = YOUTUBE_URL.matcher(text);
                if (m.matches()) {
                    return m.group(1).strip();
                }
            }
        }
        return null;
    }

    public void syncYouTubeVideos(List<YouTubeVideo> videos, List<String> categories) {
        for (String category : categories) {
            if (CollectionUtils.isEmpty(self.syncYouTubeVideos(videos, category))) {
                break;
            }
        }
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    public List<YouTubeVideo> syncYouTubeVideos(List<YouTubeVideo> missingVideos, String category) {
        if (CollectionUtils.isNotEmpty(missingVideos)) {
            LOGGER.info("Starting YouTube videos synchronization from {}...", category);
            LocalDateTime start = LocalDateTime.now();
            Pageable pageRequest = PageRequest.of(0, 500);
            Page<CommonsCategoryLinkId> page = null;
            do {
                page = commonsService.getFilesInCategory(category, pageRequest);
                for (CommonsCategoryLinkId link : page) {
                    try {
                        CommonsPage from = link.getFrom();
                        String title = from.getTitle();
                        if (title.endsWith(".ogv") || title.endsWith(".webm")) {
                            String id = findYouTubeId(commonsService.getPageContent(from));
                            if (StringUtils.isNotBlank(id)) {
                                Optional<YouTubeVideo> opt = missingVideos.stream().filter(v -> v.getId().equals(id)).findFirst();
                                if (opt.isPresent()) {
                                    missingVideos.remove(self.updateYouTubeCommonsFileName(opt.get(), title));
                                    if (missingVideos.isEmpty()) {
                                        break;
                                    }
                                }
                            } else {
                                LOGGER.warn("Cannot find YouTube video identifier for: {}", title);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Failed to get page content of " + link, e);
                    }
                }
                pageRequest = page.nextPageable();
            } while (page.hasNext() && !missingVideos.isEmpty());
            LOGGER.info("YouTube videos synchronization from {} completed in {}", category,
                    Duration.between(LocalDateTime.now(), start));
        }
        return missingVideos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public YouTubeVideo updateYouTubeCommonsFileName(YouTubeVideo video, String filename) {
        video.setCommonsFileNames(new HashSet<>(Set.of(filename)));
        return youtubeRepository.save(video);
    }
}
