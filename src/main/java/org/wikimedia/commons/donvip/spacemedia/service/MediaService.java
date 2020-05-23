package org.wikimedia.commons.donvip.spacemedia.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
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

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${update.fullres.images}")
    private boolean updateFullResImages;

    public boolean updateMedia(Media<?, ?> media, MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo)
            throws IOException {
        return updateMedia(media, originalRepo, null);
    }

    public boolean updateMedia(Media<?, ?> media, MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo, Path localPath)
            throws IOException {
        boolean result = false;
        if (cleanupDescription(media)) {
            result = true;
        }
        if (updateReadableStateAndHashes(media, localPath)) {
            result = true;
        }
        if (findCommonsFilesWithSha1(media)) {
            result = true;
        }
        if (originalRepo != null && findDuplicatesInRepository(media, originalRepo)) {
            result = true;
        }
        return result;
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
        if (perceptualHash != null && handleDuplicates(media, repo
                .findByMetadata_PhashNotNull()
                .parallelStream()
                .filter(m -> !m.getId().equals(media.getId()))
                .map(m -> new DuplicateHolder(m.getId().toString(),
                        HashHelper.similarityScore(perceptualHash, m.getMetadata().getPhash())))
                .filter(h -> h.similarityScore < perceptualThreshold)
                .map(DuplicateHolder::toDuplicate)
                .collect(Collectors.toSet()))) {
            result = true;
        }
        return result;
    }

    private static class DuplicateHolder {
        private final String mediaId;
        private final double similarityScore;

        DuplicateHolder(String mediaId, double similarityScore) {
            this.mediaId = mediaId;
            this.similarityScore = similarityScore;
        }

        Duplicate toDuplicate() {
            return new Duplicate(mediaId, similarityScore);
        }
    }

    private static boolean handleExactDuplicates(Media<?, ?> media, List<? extends Media<?, ?>> exactDuplicates) {
        return handleDuplicates(media, exactDuplicates.stream().map(d -> new Duplicate(d.getId().toString(), 0d))
                .collect(Collectors.toSet()));
    }

    private static boolean handleDuplicates(Media<?, ?> media, Set<Duplicate> duplicates) {
        boolean result = false;
        if (isNotEmpty(duplicates)
                && (isEmpty(media.getDuplicates()) || !media.getDuplicates().containsAll(duplicates))) {
            media.setIgnored(true);
            media.setIgnoredReason("Already present in main repository");
            duplicates.forEach(media::addDuplicate);
            result = true;
        }
        return result;
    }

    public boolean updateReadableStateAndHashes(Media<?, ?> media, Path localPath) {
        boolean result = updateReadableStateAndHashes(media, media.getMetadata(), localPath);
        // T230284 - Processing full-res images can lead to OOM errors
        if (updateFullResImages && media instanceof FullResMedia<?, ?>) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            if (updateReadableStateAndHashes(frMedia, frMedia.getFullResMetadata(), localPath)) {
                result = true;
            }
        }
        return result;
    }

    private boolean updateReadableStateAndHashes(Media<?, ?> media, Metadata metadata, Path localPath) {
        boolean isImage = media.isImage();
        boolean result = false;
        BufferedImage bi = null;
        try {
            URL assetUrl = metadata.getAssetUrl();
            if (isImage && shouldReadImage(assetUrl, metadata)) {
                try {
                    bi = Utils.readImage(assetUrl, false);
                    if (bi != null && !Boolean.TRUE.equals(metadata.isReadableImage())) {
                        metadata.setReadableImage(Boolean.TRUE);
                        result = true;
                    }
                } catch (IOException | URISyntaxException | ImageDecodingException e) {
                    result = ignoreMedia(media, "Unreadable media", e);
                    metadata.setReadableImage(Boolean.FALSE);
                }
            }
            if (isImage && Boolean.TRUE.equals(metadata.isReadableImage()) && updatePerceptualHash(metadata, bi)) {
                result = true;
            }
            if (updateSha1(metadata, bi, localPath)) {
                result = true;
            }
        } catch (RestClientException e) {
            LOGGER.error("Error while computing hashes for {}: {}", media, e.getMessage());
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error while computing hashes for " + media, e);
        } finally {
            if (bi != null) {
                bi.flush();
            }
        }
        return result;
    }

    private static boolean shouldReadImage(URL assetUrl, Metadata metadata) {
        return assetUrl != null
                && (metadata.isReadableImage() == null || (Boolean.TRUE.equals(metadata.isReadableImage())
                        && (metadata.getPhash() == null || metadata.getSha1() == null)));
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
     * @param image {@code BufferedImage} of asset, can be null if not an image, or not computed
     * @param localPath if set, use it instead of asset URL
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     * @throws URISyntaxException if URL cannot be converted to URI
     */
    public static boolean updateSha1(Metadata metadata, BufferedImage image, Path localPath)
            throws IOException, URISyntaxException {
        if (metadata.getSha1() == null && (metadata.getAssetUrl() != null || localPath != null)) {
            metadata.setSha1(getSha1(image, localPath, metadata.getAssetUrl()));
            return true;
        }
        return false;
    }

    private static String getSha1(BufferedImage image, Path localPath, URL url) throws IOException, URISyntaxException {
        if (image != null) {
            try {
                return HashHelper.computeSha1(image);
            } catch (IOException | UnsupportedOperationException e) {
                LOGGER.warn("Error during direct SHA-1 computation ({}), retrying from {}", e.getMessage(), url);
                return HashHelper.computeSha1(url);
            }
        } else if (localPath != null) {
            return HashHelper.computeSha1(localPath);
        } else {
            return HashHelper.computeSha1(url);
        }
    }

    /**
     * Computes the perceptual hash of an image, if required.
     *
     * @param metadata  image media metadata
     * @param image     {@code BufferedImage} of asset, can be null if not computed
     * @return {@code true} if media has been updated with computed perceptual hash
     *         and must be persisted
     */
    public static boolean updatePerceptualHash(Metadata metadata, BufferedImage image) {
        if (metadata.getPhash() == null && image != null && metadata.getAssetUrl() != null) {
            metadata.setPerceptualHash(HashHelper.computePerceptualHash(image));
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

    @Transactional
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
