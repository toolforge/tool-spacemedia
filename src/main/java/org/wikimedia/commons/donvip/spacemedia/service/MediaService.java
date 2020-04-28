package org.wikimedia.commons.donvip.spacemedia.service;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Duplicate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private static final List<String> STRINGS_TO_REMOVE = Arrays.asList(" rel=\"noreferrer nofollow\"");

    private static final List<String> STRINGS_TO_REPLACE_BY_SPACE = Arrays.asList("&nbsp;", "  ");

    @Autowired
    private CommonsService commonsService;

    @Value("${perceptual.threshold:0.07}")
    private double perceptualThreshold;

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
        BigInteger phash = media.getMetadata().getPhash();
        if (phash != null && handleExactDuplicates(media,
                repo instanceof FullResMediaRepository<?, ?, ?>
                        ? ((FullResMediaRepository<?, ?, ?>) repo).findByMetadata_PhashOrFullResMetadata_Phash(phash)
                        : repo.findByMetadata_Phash(phash))) {
            result = true;
        }
        // Find almost duplicates by perceptual hash
        if (phash != null && handleDuplicates(media, repo.findByMetadata_PhashNotNull()
                .parallelStream()
                .filter(m -> !m.getId().equals(media.getId()))
                .map(m -> new DuplicateHolder(m.getId().toString(), HashHelper.similarityScore(phash, m.getMetadata().getPhash())))
                .filter(h -> h.similarityScore < perceptualThreshold)
                .map(DuplicateHolder::toDuplicate)
                .collect(Collectors.toList()))) {
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
                .collect(Collectors.toList()));
    }

    private static boolean handleDuplicates(Media<?, ?> media, List<Duplicate> duplicates) {
        boolean result = false;
        if (isNotEmpty(duplicates) && !media.getDuplicates().containsAll(duplicates)) {
            media.setIgnored(true);
            media.setIgnoredReason("Already present in main repository.");
            duplicates.forEach(media::addDuplicate);
            result = true;
        }
        return result;
    }

    public boolean updateReadableStateAndHashes(Media<?, ?> media, Path localPath) {
        boolean result = false;
        BufferedImage bi = null;
        BufferedImage biFullRes = null;
        try {
            boolean isImage = media.isImage();
            Metadata metadata = media.getMetadata();
            URL assetUrl = metadata.getAssetUrl();
            if (isImage && metadata.isReadableImage() == null && assetUrl != null) {
                try {
                    bi = Utils.readImage(assetUrl, false);
                    metadata.setReadableImage(Boolean.TRUE);
                } catch (IOException | URISyntaxException | ImageDecodingException e) {
                    result = ignoreMedia(media, "Unreadable media", e);
                    metadata.setReadableImage(Boolean.FALSE);
                }
            }
            if (media instanceof FullResMedia<?, ?>) {
                FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
                Metadata frMetadata = frMedia.getFullResMetadata();
                URL frAssetUrl = frMetadata.getAssetUrl();
                if (isImage && frMetadata.isReadableImage() == null && frAssetUrl != null) {
                    try {
                        biFullRes = Utils.readImage(frAssetUrl, false);
                        frMetadata.setReadableImage(Boolean.TRUE);
                    } catch (IOException | URISyntaxException | ImageDecodingException e) {
                        result = ignoreMedia(frMedia, "Unreadable full-res media", e);
                        frMetadata.setReadableImage(Boolean.FALSE);
                    }
                }
            }
            if (isImage && Boolean.TRUE.equals(metadata.isReadableImage())
                    && computePerceptualHash(media, bi, biFullRes)) {
                result = true;
            }
            if (computeSha1(media, bi, biFullRes, localPath)) {
                result = true;
            }
        } catch (IOException | URISyntaxException | ImageDecodingException e) {
            LOGGER.error("Error while computing hashes", e);
        } finally {
            if (bi != null) {
                bi.flush();
            }
            if (biFullRes != null) {
                biFullRes.flush();
            }
        }
        return result;
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
     * @param media media object
     * @param bi {@code BufferedImage} of main asset, can be null if not an image, or not computed 
     * @param biFullRes {@code BufferedImage} of full-res asset, can be null if not an image, or not computed
     * @param localPath if set, use it instead of asset URL 
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     * @throws URISyntaxException if URL cannot be converted to URI
     */
    public boolean computeSha1(Media<?, ?> media, BufferedImage bi, BufferedImage biFullRes, Path localPath)
            throws IOException, URISyntaxException {
        boolean result = false;
        if (updateSha1(media.getMetadata(), bi, localPath)) {
            result = true;
        }
        if (media instanceof FullResMedia) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            if (updateSha1(frMedia.getFullResMetadata(), biFullRes, localPath)) {
                result = true;
            }
        }
        return result;
    }

    private static boolean updateSha1(Metadata metadata, BufferedImage image, Path localPath)
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
            } catch (IOException e) {
                LOGGER.error("Error during SHA-1 computation", e);
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
     * @param media     image media object
     * @param bi        {@code BufferedImage} of main asset, can be null if not
     *                  computed
     * @param biFullRes {@code BufferedImage} of full-res asset, can be null if not
     *                  an image, or not computed
     * @return {@code true} if media has been updated with computed perceptual hash
     *         and must be persisted
     */
    public boolean computePerceptualHash(Media<?, ?> media, BufferedImage bi, BufferedImage biFullRes)
            throws IOException, URISyntaxException, ImageDecodingException {
        boolean result = false;
        if (updatePerceptualHash(media.getMetadata(), bi)) {
            result = true;
        }
        if (media instanceof FullResMedia) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            if (updatePerceptualHash(frMedia.getFullResMetadata(), biFullRes)) {
                result = true;
            }
        }
        return result;
    }

    private static boolean updatePerceptualHash(Metadata metadata, BufferedImage image)
            throws IOException, URISyntaxException, ImageDecodingException {
        if (metadata.getPhash() == null && metadata.getAssetUrl() != null) {
            metadata.setPhash(HashHelper.computePerceptualHash(image, metadata.getAssetUrl()));
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
}
