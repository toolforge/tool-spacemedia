package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.ImageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.WikiPage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.ImageUtils;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private static final List<String> STRINGS_TO_REMOVE = Arrays.asList(" rel=\"noreferrer nofollow\"");

    private static final Map<String, String> STRINGS_TO_REPLACE = Map.of("&nbsp;", " ", "  ", " ", "â€™", "’", "ÔÇÖ",
            "’", "ÔÇ£", "«", "ÔÇØ", "»");

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

    @Autowired
    private FileMetadataRepository metadataRepository;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${perceptual.threshold.identicalid}")
    private double perceptualThresholdIdenticalId;

    @Value("${update.fullres.images}")
    private boolean updateFullResImages;

    private Set<String> blockListIgnoredTerms;
    private Set<String> copyrightsBlocklist;
    private Set<String> photographersBlocklist;

    @PostConstruct
    public void init() throws IOException {
        blockListIgnoredTerms = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.terms.csv"));
        copyrightsBlocklist = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.copyrights.csv"));
        photographersBlocklist = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.photographers.csv"));
    }

    public <M extends Media<?, ?>> MediaUpdateResult updateMedia(M media, Iterable<String> stringsToRemove,
            boolean forceUpdate, UrlResolver<M> urlResolver) throws IOException {
        return updateMedia(media, stringsToRemove, forceUpdate, urlResolver, true, true, null);
    }

    public <M extends Media<?, ?>> MediaUpdateResult updateMedia(M media, Iterable<String> stringsToRemove,
            boolean forceUpdate, UrlResolver<M> urlResolver, boolean checkBlocklist, boolean includeByPerceptualHash,
            Path localPath) throws IOException {
        boolean result = false;
        if (cleanupDescription(media, stringsToRemove)) {
            result = true;
        }
        MediaUpdateResult ur = updateReadableStateAndHashes(media, localPath, urlResolver, forceUpdate);
        if (ur.getResult()) {
            result = true;
        }
        if (findCommonsFiles(media.getMetadata(), media.getIdUsedInCommons(), includeByPerceptualHash)) {
            result = true;
        }
        if (checkBlocklist && !Boolean.TRUE.equals(media.isIgnored()) && belongsToBlocklist(media)) {
            result = true;
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    protected boolean belongsToBlocklist(Media<?, ?> media) {
        String titleAndDescription = "";
        if (media.getTitle() != null) {
            titleAndDescription += media.getTitle().toLowerCase(Locale.ENGLISH);
        }
        if (media.getDescription() != null) {
            titleAndDescription += media.getDescription().toLowerCase(Locale.ENGLISH);
        }
        if (!titleAndDescription.isEmpty()) {
            titleAndDescription = titleAndDescription.replace("\r\n", " ").replace('\t', ' ').replace('\r', ' ')
                    .replace('\n', ' ');
            String ignoredTerms = blockListIgnoredTerms.parallelStream().filter(titleAndDescription::contains).sorted()
                    .collect(joining(","));
            if (!ignoredTerms.isEmpty()) {
                return ignoreMedia(media, "Title or description contains term(s) in block list: " + ignoredTerms);
            }
        }
        for (FileMetadata metadata : media.getMetadata()) {
            if (metadata != null && metadata.getExif() != null
                    && (metadata.getExif().getPhotographers().anyMatch(this::isPhotographerBlocklisted)
                            || metadata.getExif().getCopyrights().anyMatch(this::isCopyrightBlocklisted))) {
                return ignoreMedia(media,
                        "Probably non-free image (EXIF photographer/copyright blocklisted) : "
                                + metadata.getExif().getPhotographers().sorted().toList() + " / "
                                + metadata.getExif().getCopyrights().sorted().toList());
            }
        }
        return false;
    }

    public boolean isCopyrightBlocklisted(String copyright) {
        return copyrightsBlocklist.stream().anyMatch(copyright::contains);
    }

    public boolean isPhotographerBlocklisted(String photographer) {
        String normalizedPhotographer = photographer.toLowerCase(Locale.ENGLISH).replace(' ', '_');
        return photographersBlocklist.stream().anyMatch(normalizedPhotographer::startsWith);
    }

    public <M extends Media<?, ?>> MediaUpdateResult updateReadableStateAndHashes(M media, Path localPath,
            UrlResolver<M> urlResolver, boolean forceUpdateOfHashes) {
        boolean result = false;
        Exception exception = null;
        for (FileMetadata metadata : media.getMetadata()) {
            MediaUpdateResult ur = updateReadableStateAndHashes(media, metadata, localPath, urlResolver,
                    forceUpdateOfHashes);
            if (ur.getResult()) {
                result = true;
            }
            if (ur.getException() != null) {
                exception = ur.getException();
            }
        }
        // T230284 - Processing full-res images can lead to OOM errors
        return new MediaUpdateResult(result, exception);
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

    private <M extends Media<?, ?>> MediaUpdateResult updateReadableStateAndHashes(M media, FileMetadata metadata,
            Path localPath, UrlResolver<M> urlResolver, boolean forceUpdateOfHashes) {
        boolean isImage = metadata.isImage();
        boolean result = false;
        BufferedImage bi = null;
        try {
            URL assetUrl = urlResolver.resolveDownloadUrl(media, metadata);
            if (isImage && shouldReadImage(assetUrl, metadata, forceUpdateOfHashes)) {
                try {
                    Pair<BufferedImage, Long> pair = ImageUtils.readImage(assetUrl, false, true);
                    bi = pair.getLeft();
                    if (bi != null) {
                        if (!Boolean.TRUE.equals(metadata.isReadableImage())) {
                            metadata.setReadableImage(Boolean.TRUE);
                            result = true;
                        }
                        if (bi.getWidth() > 0 && bi.getHeight() > 0 && !metadata.hasValidDimensions()) {
                            metadata.setImageDimensions(new ImageDimensions(bi.getWidth(), bi.getHeight()));
                            result = true;
                        }
                    }
                    Long contentLength = pair.getRight();
                    if (contentLength > 0 && metadata.getSize() == null) {
                        metadata.setSize(contentLength);
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
            if (updateSha1(media, metadata, localPath, urlResolver, forceUpdateOfHashes)) {
                result = true;
            }
        } catch (RestClientException e) {
            LOGGER.error("Error while computing hashes for {}: {}", media, e.getMessage());
            return new MediaUpdateResult(result, e);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error while computing hashes for {}", media, e);
            return new MediaUpdateResult(result, e);
        } finally {
            if (bi != null) {
                bi.flush();
            }
        }
        if (result) {
            metadataRepository.save(metadata);
        }
        return new MediaUpdateResult(result, null);
    }

    private static boolean shouldReadImage(URL assetUrl, FileMetadata metadata, boolean forceUpdateOfHashes) {
        return assetUrl != null
                && (metadata.isReadableImage() == null || (Boolean.TRUE.equals(metadata.isReadableImage())
                        && (metadata.getPhash() == null || metadata.getSha1() == null || !metadata.hasValidDimensions()
                                || forceUpdateOfHashes)));
    }

    public static boolean ignoreMedia(Media<?, ?> media, String reason) {
        return ignoreMedia(media, reason, null);
    }

    public static boolean ignoreMedia(Media<?, ?> media, String reason, Exception e) {
        if (e != null) {
            LOGGER.warn("Ignored {} for reason {}", media, reason, e);
        } else {
            LOGGER.warn("Ignored {} for reason {}", media, reason);
        }
        media.setIgnored(Boolean.TRUE);
        media.setIgnoredReason(reason + (e != null ? ": " + e.getMessage() : ""));
        return true;
    }

    public static boolean cleanupDescription(Media<?, ?> media, Iterable<String> stringsToRemove) {
        boolean result = false;
        if (StringUtils.isNotBlank(media.getDescription())) {
            if (stringsToRemove != null) {
                for (String toRemove : stringsToRemove) {
                    if (media.getDescription().contains(toRemove)) {
                        media.setDescription(media.getDescription().replace(toRemove, "").trim());
                        result = true;
                    }
                }
            }
            for (String toRemove : STRINGS_TO_REMOVE) {
                if (media.getDescription().contains(toRemove)) {
                    media.setDescription(media.getDescription().replace(toRemove, ""));
                    result = true;
                }
            }
            for (Entry<String, String> toReplace : STRINGS_TO_REPLACE.entrySet()) {
                while (media.getDescription().contains(toReplace.getKey())) {
                    media.setDescription(media.getDescription().replace(toReplace.getKey(), toReplace.getValue()));
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Computes the media SHA-1.
     *
     * @param metadata media object
     * @param metadata media object metadata
     * @param localPath if set, use it instead of asset URL
     * @param urlResolver URL download resolver
     * @param forceUpdate {@code true} to force update of an existing hash
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     * @throws URISyntaxException if URL cannot be converted to URI
     */
    public <M extends Media<?, ?>> boolean updateSha1(M media, FileMetadata metadata, Path localPath,
            UrlResolver<M> urlResolver, boolean forceUpdate) throws IOException, URISyntaxException {
        if ((metadata.getSha1() == null || forceUpdate) && (metadata.getAssetUrl() != null || localPath != null)) {
            metadata.setSha1(getSha1(localPath, urlResolver.resolveDownloadUrl(media, metadata)));
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
    public boolean updatePerceptualHash(FileMetadata metadata, BufferedImage image, boolean forceUpdate) {
        if (image != null && (metadata.getPhash() == null || forceUpdate)) {
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

    public boolean findCommonsFiles(List<FileMetadata> metadata, String idUsedInCommons,
            boolean includeByPerceptualHash) throws IOException {
        return findCommonsFilesWithSha1(metadata) || (includeByPerceptualHash
                && (findCommonsFilesWithPhash(metadata, true)
                        || findCommonsFilesWithIdAndPhash(metadata, idUsedInCommons)));
    }

    /**
     * Looks for Wikimedia Commons files matching the metadata SHA-1, if required.
     *
     * @param metadatas list of file metadata objects
     * @return {@code true} if at least one metadata has been updated with list of
     *         Wikimedia Commons files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithSha1(List<FileMetadata> metadatas) throws IOException {
        boolean result = false;
        for (FileMetadata metadata : metadatas) {
            result |= findCommonsFilesWithSha1(metadata);
        }
        return result;
    }

    private boolean findCommonsFilesWithSha1(FileMetadata metadata) throws IOException {
        String sha1 = metadata.getSha1();
        if (sha1 != null && isEmpty(metadata.getCommonsFileNames())) {
            Set<String> files = commonsService.findFilesWithSha1(sha1);
            if (!files.isEmpty()) {
                return saveNewMetadataCommonsFileNames(metadata, files);
            }
        }
        return false;
    }

    /**
     * Looks for Wikimedia Commons files matching exactly the metadata perceptual
     * hash, if required.
     *
     * @param metadatas       list of file metadata objects
     * @param excludeSelfSha1 whether to exclude metadata's own sha1 from search
     * @return {@code true} if at least one metadata has been updated with list of
     *         Wikimedia Commons files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithPhash(List<FileMetadata> metadatas, boolean excludeSelfSha1) throws IOException {
        boolean result = false;
        for (FileMetadata metadata : metadatas) {
            if (findCommonsFilesWithPhash(metadata, excludeSelfSha1)) {
                result = true;
            }
        }
        return result;
    }

    private boolean findCommonsFilesWithPhash(FileMetadata metadata, boolean excludeSelfSha1) throws IOException {
        String phash = metadata.getPhash();
        if (phash != null && isEmpty(metadata.getCommonsFileNames())) {
            List<String> sha1s = hashRepository.findSha1ByPhash(phash);
            if (excludeSelfSha1) {
                sha1s.remove(CommonsService.base36Sha1(metadata.getSha1()));
            }
            if (!sha1s.isEmpty()) {
                Set<String> files = commonsService.findFilesWithSha1(sha1s);
                if (!files.isEmpty()) {
                    return saveNewMetadataCommonsFileNames(metadata, files);
                }
            }
        }
        return false;
    }

    public boolean findCommonsFilesWithIdAndPhash(List<FileMetadata> metadatas, String idUsedInCommons)
            throws IOException {
        boolean result = false;
        Collection<WikiPage> images = commonsService.searchImages(idUsedInCommons);
        if (!images.isEmpty()) {
            for (FileMetadata metadata : metadatas) {
                if (findCommonsFilesWithIdAndPhash(images, metadata)) {
                    result = true;
                }
            }
        }
        return result;
    }

    public List<String> findSmallerCommonsFilesWithIdAndPhash(Media<?, ?> media, FileMetadata metadata) throws IOException {
        return findCommonsFilesWithIdAndPhashFiltered(commonsService.searchImages(media.getIdUsedInCommons()), metadata,
                MediaService::filterBySameMimeAndSmallerSize);
    }

    private boolean findCommonsFilesWithIdAndPhash(Collection<WikiPage> images, FileMetadata metadata) {
        List<String> filenames = findCommonsFilesWithIdAndPhashFiltered(images, metadata,
                MediaService::filterBySameMimeAndLargerOrEqualSize);
        return !filenames.isEmpty() && saveNewMetadataCommonsFileNames(metadata, new HashSet<>(filenames));
    }

    private List<String> findCommonsFilesWithIdAndPhashFiltered(Collection<WikiPage> images, FileMetadata metadata,
            BiPredicate<FileMetadata, ImageInfo> filter) {
        List<String> filenames = new ArrayList<>();
        if (metadata.getPhash() != null && isEmpty(metadata.getCommonsFileNames())) {
            for (WikiPage image : images.stream().filter(i -> filter.test(metadata, i.getImageInfo()[0])).toList()) {
                String filename = image.getTitle().replace("File:", "").replace(' ', '_');
                String sha1base36 = CommonsService.base36Sha1(image.getImageInfo()[0].getSha1());
                Optional<HashAssociation> hash = hashRepository.findById(sha1base36);
                if (hash.isEmpty()) {
                    hash = Optional.ofNullable(commonsService.computeAndSaveHash(sha1base36, filename));
                }
                double score = HashHelper.similarityScore(metadata.getPhash(),
                        hash.orElseThrow(() -> new IllegalStateException("No hash for " + sha1base36)).getPhash());
                if (score <= perceptualThresholdIdenticalId) {
                    LOGGER.info("Found match ({}) between {} and {}", score, metadata, image);
                    filenames.add(filename);
                } else if (hash.isPresent() && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No match between {} and {} / {} -> {}", metadata, image, hash,
                            HashHelper.similarityScore(metadata.getPhash(), hash.get().getPhash()));
                }
            }
        }
        return filenames;
    }

    private static boolean filterBySameMimeAndLargerOrEqualSize(FileMetadata metadata, ImageInfo imageInfo) {
        return StringUtils.equals(metadata.getMime(), imageInfo.getMime()) && metadata.getSize() != null
                && metadata.getSize() <= imageInfo.getSize();
    }

    private static boolean filterBySameMimeAndSmallerSize(FileMetadata metadata, ImageInfo imageInfo) {
        return StringUtils.equals(metadata.getMime(), imageInfo.getMime()) && metadata.getSize() != null
                && metadata.getSize() > imageInfo.getSize();
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
            if (isEmpty(self.syncYouTubeVideos(videos, category))) {
                break;
            }
        }
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    public List<YouTubeVideo> syncYouTubeVideos(List<YouTubeVideo> missingVideos, String category) {
        if (isNotEmpty(missingVideos)) {
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
                        LOGGER.error("Failed to get page content of {}", link, e);
                    }
                }
                pageRequest = page.nextPageable();
            } while (page.hasNext() && !missingVideos.isEmpty());
            LOGGER.info("YouTube videos synchronization from {} completed in {}", category, Utils.durationInSec(start));
        }
        return missingVideos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public YouTubeVideo updateYouTubeCommonsFileName(YouTubeVideo video, String filename) {
        saveNewMetadataCommonsFileNames(video.getUniqueMetadata(), new HashSet<>(Set.of(filename)));
        return youtubeRepository.save(video);
    }

    private boolean saveNewMetadataCommonsFileNames(FileMetadata metadata, Set<String> commonsFileNames) {
        metadata.setCommonsFileNames(commonsFileNames);
        metadataRepository.save(metadata);
        return true;
    }
}
