package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.text.StringEscapeUtils.unescapeXml;
import static org.wikimedia.commons.donvip.spacemedia.utils.ImageUtils.readImage;
import static org.wikimedia.commons.donvip.spacemedia.utils.ImageUtils.readImageMetadata;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.ImageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.WikiPage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDescription;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;

@Service
public class MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private static final List<String> STRINGS_TO_REMOVE = Arrays.asList(" rel=\"noreferrer nofollow\"");

    private static final Map<String, String> STRINGS_TO_REPLACE = Map.of("&nbsp;", " ", "  ", " ", "â€™", "’", "ÔÇÖ",
            "’", "ÔÇ£", "«", "ÔÇØ", "»");

    @Autowired
    private CommonsService commonsService;

    @Autowired
    private HashAssociationRepository hashRepository;

    @Autowired
    private FileMetadataRepository metadataRepository;

    @Autowired
    private ExifMetadataRepository exifRepository;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${perceptual.threshold.identicalid}")
    private double perceptualThresholdIdenticalId;

    @Value("${update.fullres.images}")
    private boolean updateFullResImages;

    @Value("${ignored.phash}")
    private Set<String> ignoredPhash;

    @Value("${ignored.sha1}")
    private Set<String> ignoredSha1;

    private Set<String> blockListIgnoredTerms;
    private Set<String> copyrightsBlocklist;
    private Set<String> photographersBlocklist;

    @PostConstruct
    public void init() throws IOException {
        blockListIgnoredTerms = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.terms.csv"));
        copyrightsBlocklist = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.copyrights.csv"));
        photographersBlocklist = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.photographers.csv"));
    }

    public <M extends Media> MediaUpdateResult updateMedia(M media, Iterable<String> stringsToRemove,
            boolean forceUpdate, UrlResolver<M> urlResolver) throws IOException {
        return updateMedia(media, stringsToRemove, forceUpdate, urlResolver, true, true, false, null);
    }

    public <M extends Media> MediaUpdateResult updateMedia(M media, Iterable<String> stringsToRemove,
            boolean forceUpdate, UrlResolver<M> urlResolver, boolean checkBlocklist, boolean includeByPerceptualHash,
            boolean ignoreExifMetadata, Path localPath) throws IOException {
        boolean result = false;
        if (cleanupDescription(media, stringsToRemove)) {
            LOGGER.info("Description has been cleaned up for {}", media);
            result = true;
        }
        MediaUpdateResult ur = updateReadableStateAndHashes(media, localPath, urlResolver, forceUpdate,
                ignoreExifMetadata);
        if (ur.getResult()) {
            LOGGER.info("Readable state and/or hashes have been updated for {}", media);
            result = true;
        }
        if (findCommonsFiles(media.getMetadata(), media.getSearchTermsInCommons(), includeByPerceptualHash)) {
            LOGGER.info("Commons files have been updated for {}", media);
            result = true;
        }
        if (checkBlocklist && !media.isIgnored() && belongsToBlocklist(media)) {
            LOGGER.info("Blocklist has been trigerred for {}", media);
            result = true;
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    protected boolean belongsToBlocklist(Media media) {
        StringBuilder sb = new StringBuilder();
        if (media.getTitle() != null) {
            sb.append(media.getTitle().toLowerCase(ENGLISH));
        }
        media.getDescriptions().stream().forEach(x -> sb.append(' ').append(x.toLowerCase(ENGLISH)));
        String titleAndDescription = sb.toString().trim();
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
        String normalizedPhotographer = photographer.toLowerCase(ENGLISH).replace(' ', '_');
        return photographersBlocklist.stream().anyMatch(normalizedPhotographer::startsWith);
    }

    public <M extends Media> MediaUpdateResult updateReadableStateAndHashes(M media, Path localPath,
            UrlResolver<M> urlResolver, boolean forceUpdateOfHashes, boolean ignoreExifMetadata) {
        boolean result = false;
        Exception exception = null;
        for (FileMetadata metadata : media.getMetadata()) {
            if (metadata.isIgnored() != Boolean.TRUE) {
                MediaUpdateResult ur = updateReadableStateAndHashes(media, metadata, localPath, urlResolver,
                        forceUpdateOfHashes, ignoreExifMetadata);
                result |= ur.getResult();
                if (ur.getException() != null) {
                    exception = ur.getException();
                }
                if (ur.getResult()) {
                    LOGGER.info("Readable state and/or hashes have been updated for {}", metadata);
                }
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

    private <M extends Media> MediaUpdateResult updateReadableStateAndHashes(M media, FileMetadata metadata,
            Path localPath, UrlResolver<M> urlResolver, boolean forceUpdateOfHashes, boolean ignoreExifMetadata) {
        boolean isImage = metadata.isImage();
        boolean result = false;
        BufferedImage bi = null;
        try {
            URL assetUrl = urlResolver.resolveDownloadUrl(media, metadata);
            if (isImage && shouldReadImage(assetUrl, metadata, forceUpdateOfHashes)) {
                try {
                    Pair<BufferedImage, Long> pair = readImage(assetUrl, false, true);
                    bi = pair.getLeft();
                    if (bi != null) {
                        if (!Boolean.TRUE.equals(metadata.isReadableImage())) {
                            metadata.setReadableImage(Boolean.TRUE);
                            LOGGER.info("Readable state has been updated to {} for {}", Boolean.TRUE, metadata);
                            result = true;
                        }
                        if (bi.getWidth() > 0 && bi.getHeight() > 0 && !metadata.hasValidDimensions()) {
                            metadata.setImageDimensions(new ImageDimensions(bi.getWidth(), bi.getHeight()));
                            LOGGER.info("Image dimensions have been updated for {}", metadata);
                            result = true;
                        }
                    }
                    Long contentLength = pair.getRight();
                    if (contentLength > 0 && !metadata.hasSize()) {
                        metadata.setSize(contentLength);
                        LOGGER.info("Size has been updated for {}", metadata);
                        result = true;
                    }
                } catch (IOException | ImageDecodingException e) {
                    result = ignoreMetadata(metadata, "Unreadable file", e);
                    metadata.setReadableImage(Boolean.FALSE);
                    LOGGER.info("Readable state has been updated to {} for {}", Boolean.FALSE, metadata);
                }
            }
            if (isImage && Boolean.TRUE.equals(metadata.isReadableImage())
                    && updatePerceptualHash(metadata, bi, forceUpdateOfHashes)) {
                LOGGER.info("Perceptual hash has been updated for {}", metadata);
                result = true;
            }
            if (bi != null) {
                bi.flush();
                bi = null;
            }
            if (updateSha1(media, metadata, localPath, urlResolver, forceUpdateOfHashes)) {
                LOGGER.info("SHA1 hash has been updated for {}", metadata);
                result = true;
            }
            if (isImage && !ignoreExifMetadata && updateExifMetadata(metadata)) {
                LOGGER.info("EXIF metadata has been updated for {}", metadata);
                result = true;
            }
        } catch (RestClientException e) {
            LOGGER.error("Error while computing hashes for {}: {}", media, e.getMessage());
            return new MediaUpdateResult(result, e);
        } catch (IOException e) {
            LOGGER.error("Error while computing hashes for {}", media, e);
            return new MediaUpdateResult(result, e);
        } finally {
            if (bi != null) {
                bi.flush();
            }
        }
        if (result) {
            LOGGER.info("Saving {}", metadata);
            metadataRepository.save(metadata);
        }
        return new MediaUpdateResult(result, null);
    }

    private static boolean shouldReadImage(URL assetUrl, FileMetadata metadata, boolean forceUpdateOfHashes) {
        return assetUrl != null
                && (metadata.isReadableImage() == null || (Boolean.TRUE.equals(metadata.isReadableImage())
                        && (!metadata.hasPhash() || !metadata.hasSha1() || !metadata.hasValidDimensions()
                                || !metadata.hasSize() || forceUpdateOfHashes)));
    }

    public static boolean ignoreMedia(Media media, String reason) {
        return ignoreMedia(media, reason, null);
    }

    public static boolean ignoreMedia(Media media, String reason, Exception e) {
        media.getMetadataStream().forEach(fm -> ignoreMetadata(fm, reason, e));
        return true;
    }

    public static boolean ignoreMetadata(FileMetadata fm, String reason) {
        return ignoreMetadata(fm, reason, null);
    }

    public static boolean ignoreMetadata(FileMetadata fm, String reason, Exception e) {
        if (e != null) {
            LOGGER.warn("Ignored {} for reason {}", fm, reason, e);
        } else {
            LOGGER.warn("Ignored {} for reason {}", fm, reason);
        }
        fm.setIgnored(Boolean.TRUE);
        fm.setIgnoredReason(reason + (e != null ? ": " + e.getMessage() : ""));
        return true;
    }

    public static boolean cleanupDescription(Media media, Iterable<String> stringsToRemove) {
        boolean result = false;
        for (MediaDescription md : media.getDescriptionObjects()) {
            String description = md.getDescription();
            if (isNotBlank(description)) {
                description = unescapeXml(description);
                if (stringsToRemove != null) {
                    for (String toRemove : stringsToRemove) {
                        if (description.contains(toRemove)) {
                            description = description.replace(toRemove, "").trim();
                        }
                    }
                }
                for (String toRemove : STRINGS_TO_REMOVE) {
                    if (description.contains(toRemove)) {
                        description = description.replace(toRemove, "");
                    }
                }
                for (Entry<String, String> toReplace : STRINGS_TO_REPLACE.entrySet()) {
                    while (description.contains(toReplace.getKey())) {
                        description = description.replace(toReplace.getKey(), toReplace.getValue());
                    }
                }
                result = !description.equals(md.getDescription());
                if (result) {
                    md.setDescription(description);
                }
            }
        }
        return result;
    }

    public boolean updateExifMetadata(FileMetadata metadata) throws IOException {
        if (metadata.getExif() == null) {
            metadata.setExif(exifRepository.save(ExifMetadata.of(readImageMetadata(metadata.getAssetUri()))));
            return true;
        }
        return false;
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
     */
    public <M extends Media> boolean updateSha1(M media, FileMetadata metadata, Path localPath,
            UrlResolver<M> urlResolver, boolean forceUpdate) throws IOException {
        if ((!metadata.hasSha1() || forceUpdate) && (metadata.getAssetUrl() != null || localPath != null)) {
            metadata.setSha1(getSha1(localPath, urlResolver.resolveDownloadUrl(media, metadata)));
            updateHashes(metadata.getSha1(), metadata.getPhash(), metadata.getMime());
            return true;
        }
        return false;
    }

    private static String getSha1(Path localPath, URL url) throws IOException {
        if (localPath != null) {
            return HashHelper.computeSha1(localPath);
        } else {
            return HashHelper.computeSha1(url);
        }
    }

    private void updateHashes(String sha1, String phash, String mime) {
        if (sha1 != null) {
            String sha1base36 = CommonsService.base36Sha1(sha1);
            if (!hashRepository.existsById(sha1base36)) {
                hashRepository.save(new HashAssociation(sha1base36, phash, mime));
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
        if (image != null && (!metadata.hasPhash() || forceUpdate)) {
            try {
                metadata.setPhash(HashHelper.encode(HashHelper.computePerceptualHash(image)));
            } catch (RuntimeException e) {
                LOGGER.error("Failed to update perceptual hash for {}", metadata, e);
            }
            updateHashes(metadata.getSha1(), metadata.getPhash(), metadata.getMime());
            return true;
        }
        return false;
    }

    public boolean findCommonsFiles(Collection<FileMetadata> metadata, Collection<String> searchTermsInCommons,
            boolean includeByPerceptualHash) throws IOException {
        return findCommonsFilesWithSha1(metadata) || (includeByPerceptualHash
                && (findCommonsFilesWithPhash(metadata, true)
                        || findCommonsFilesWithTextAndPhash(metadata, searchTermsInCommons)));
    }

    /**
     * Looks for Wikimedia Commons files matching the metadata SHA-1, if required.
     *
     * @param metadatas list of file metadata objects
     * @return {@code true} if at least one metadata has been updated with list of
     *         Wikimedia Commons files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithSha1(Collection<FileMetadata> metadatas) throws IOException {
        boolean result = false;
        for (FileMetadata metadata : metadatas) {
            result |= findCommonsFilesWithSha1(metadata);
        }
        return result;
    }

    private boolean findCommonsFilesWithSha1(FileMetadata metadata) throws IOException {
        if (shouldSearchBySha1(metadata)) {
            Set<String> files = commonsService.findFilesWithSha1(metadata.getSha1());
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
    public boolean findCommonsFilesWithPhash(Collection<FileMetadata> metadatas, boolean excludeSelfSha1)
            throws IOException {
        boolean result = false;
        for (FileMetadata metadata : metadatas) {
            if (findCommonsFilesWithPhash(metadata, excludeSelfSha1)) {
                result = true;
            }
        }
        return result;
    }

    private boolean findCommonsFilesWithPhash(FileMetadata metadata, boolean excludeSelfSha1) throws IOException {
        if (shouldSearchByPhash(metadata)) {
            List<String> sha1s = hashRepository.findSha1ByPhashAndMime(metadata.getPhash(), metadata.getMime());
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

    public boolean findCommonsFilesWithTextAndPhash(Collection<FileMetadata> metadatas, Collection<String> texts)
            throws IOException {
        boolean result = false;
        for (String text : texts) {
            if (StringUtils.isNotBlank(text)) {
                Collection<WikiPage> images = commonsService.searchImages(text.strip());
                if (!images.isEmpty()) {
                    for (FileMetadata metadata : metadatas) {
                        if (findCommonsFilesWithIdAndPhash(images, metadata)) {
                            result = true;
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<String> findSmallerCommonsFilesWithIdAndPhash(Media media, FileMetadata metadata) throws IOException {
        List<String> result = new ArrayList<>();
        for (String idUsedInCommons : media.getIdUsedInCommons()) {
            result.addAll(findCommonsFilesWithIdAndPhashFiltered(commonsService.searchImages(idUsedInCommons), metadata,
                    MediaService::filterBySameMimeAndSmallerSize));
        }
        return result;
    }

    private boolean findCommonsFilesWithIdAndPhash(Collection<WikiPage> images, FileMetadata metadata) {
        List<String> filenames = findCommonsFilesWithIdAndPhashFiltered(images, metadata,
                MediaService::filterBySameMimeAndLargerOrEqualSizeOrLargerOrEqualDimensions);
        return !filenames.isEmpty() && saveNewMetadataCommonsFileNames(metadata, new HashSet<>(filenames));
    }

    private List<String> findCommonsFilesWithIdAndPhashFiltered(Collection<WikiPage> images, FileMetadata metadata,
            BiPredicate<FileMetadata, ImageInfo> filter) {
        List<String> filenames = new ArrayList<>();
        if (shouldSearchByPhash(metadata)) {
            List<WikiPage> list = images.stream().filter(i -> filter.test(metadata, i.getImageInfo()[0])).toList();
            LOGGER.debug("Searching match by id/phash for {} => filtered to {}", images, list);
            for (WikiPage image : list) {
                String filename = image.getTitle().replace("File:", "").replace(' ', '_');
                String sha1base36 = CommonsService.base36Sha1(image.getImageInfo()[0].getSha1());
                Optional<HashAssociation> hash = hashRepository.findById(sha1base36);
                if (hash.isEmpty()) {
                    hash = Optional.ofNullable(commonsService.computeAndSaveHash(sha1base36, filename,
                            FileMetadata.getMime(filename.substring(filename.lastIndexOf('.') + 1))));
                }
                String phash = hash.orElseThrow(() -> new IllegalStateException("No hash for " + sha1base36)).getPhash();
                if (phash != null) {
                    double score = HashHelper.similarityScore(metadata.getPhash(), phash);
                    if (score <= perceptualThresholdIdenticalId) {
                        LOGGER.info("Found match ({}) between {} and {}", score, metadata, image);
                        filenames.add(filename);
                    } else if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("No match between {} and {} / {} -> {}", metadata, image, hash, score);
                    }
                }
            }
        }
        return filenames;
    }

    private boolean shouldSearchByPhash(FileMetadata metadata) {
        return metadata.hasPhash() && !ignoredPhash.contains(metadata.getPhash())
                && isEmpty(metadata.getCommonsFileNames());
    }

    private boolean shouldSearchBySha1(FileMetadata metadata) {
        return metadata.hasSha1() && !ignoredSha1.contains(metadata.getSha1())
                && isEmpty(metadata.getCommonsFileNames());
    }

    private static boolean filterBySameMimeAndLargerOrEqualSizeOrLargerOrEqualDimensions(FileMetadata metadata,
            ImageInfo imageInfo) {
        return StringUtils.equals(metadata.getMime(), imageInfo.getMime()) && metadata.hasSize()
                && (metadata.getSize() <= imageInfo.getSize()
                        || areLargerOrEqualDimensions(metadata.getImageDimensions(), imageInfo));
    }

    private static boolean areLargerOrEqualDimensions(ImageDimensions dims, ImageInfo imageInfo) {
        return dims != null && dims.getWidth() <= imageInfo.getWidth() && dims.getHeight() <= imageInfo.getHeight();
    }

    private static boolean filterBySameMimeAndSmallerSize(FileMetadata metadata, ImageInfo imageInfo) {
        return StringUtils.equals(metadata.getMime(), imageInfo.getMime()) && metadata.hasSize()
                && metadata.getSize() > imageInfo.getSize();
    }

    public boolean saveNewMetadataCommonsFileNames(FileMetadata metadata, Set<String> commonsFileNames) {
        LOGGER.info("Saving new commons filenames {} for {}", commonsFileNames, metadata);
        metadata.setCommonsFileNames(commonsFileNames);
        metadataRepository.save(metadata);
        return true;
    }

    public <T> void useMapping(Set<String> result, String key, Set<T> items,
            Map<String, Map<String, String>> mappings, Function<T, String> keyFunction) {
        if (CollectionUtils.isNotEmpty(items)) {
            Map<String, String> mapping = mappings.get(key);
            if (MapUtils.isNotEmpty(mapping)) {
                for (T item : items) {
                    String cats = mapping.get(keyFunction.apply(item));
                    if (isNotBlank(cats)) {
                        Arrays.stream(cats.split(";")).map(String::trim).forEach(result::add);
                    }
                }
            }
        }
    }
}
