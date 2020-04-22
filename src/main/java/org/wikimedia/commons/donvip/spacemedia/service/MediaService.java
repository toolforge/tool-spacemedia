package org.wikimedia.commons.donvip.spacemedia.service;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class MediaService {

    private static final List<String> STRINGS_TO_REMOVE = Arrays.asList(" rel=\"noreferrer nofollow\"");

    private static final List<String> STRINGS_TO_REPLACE_BY_SPACE = Arrays.asList("&nbsp;", "  ");

    @Autowired
    private CommonsService commonsService;

    public boolean updateMedia(Media<?, ?> media, MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo)
            throws IOException, URISyntaxException {
        boolean result = false;
        if (cleanupDescription(media)) {
            result = true;
        }
        if (computeSha1(media)) {
            result = true;
        }
        if (findCommonsFilesWithSha1(media)) {
            result = true;
        }
        if (originalRepo != null) {
            List<? extends Media<?, ?>> originals = originalRepo.findBySha1(media.getSha1());
            if (isNotEmpty(originals)) {
                Media<?, ?> original = originals.get(0);
                if (!Boolean.TRUE.equals(media.isIgnored())) {
                    media.setIgnored(true);
                    media.setIgnoredReason("Already present in main repository.");
                    result = true;
                }
                if (!Objects.equals(media.getCommonsFileNames(), original.getCommonsFileNames())) {
                    media.setCommonsFileNames(original.getCommonsFileNames());
                    if (media instanceof FullResMedia && original instanceof FullResMedia) {
                        ((FullResMedia<?, ?>) media).setFullResCommonsFileNames(
                                ((FullResMedia<?, ?>) original).getFullResCommonsFileNames());
                    }
                    result = true;
                }
            }
        }
        return result;
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
     * Computes the media SHA-1, if required, and updates the media object accordingly.
     * 
     * @param media    media object
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     * @throws URISyntaxException if URL cannot be converted to URI
     */
    public boolean computeSha1(Media<?, ?> media) throws IOException, URISyntaxException {
        boolean result = false;
        if (media.getSha1() == null && media.getAssetUrl() != null) {
            media.setSha1(Utils.computeSha1(media.getAssetUrl()));
            result = true;
        }
        if (media instanceof FullResMedia) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            if (frMedia.getFullResSha1() == null && frMedia.getFullResAssetUrl() != null) {
                frMedia.setFullResSha1(Utils.computeSha1(frMedia.getFullResAssetUrl()));
                result = true;
            }
        }
        return result;
    }

    /**
     * Looks for Wikimedia Commons files matching the media SHA-1, if required, and
     * updates the media object accordingly.
     * 
     * @param media media object
     * @return {@code true} if media has been updated with list of Wikimedia Commons
     *         files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithSha1(Media<?, ?> media) throws IOException {
        boolean result = false;
        if (media.getSha1() != null && isEmpty(media.getCommonsFileNames())) {
            Set<String> files = commonsService.findFilesWithSha1(media.getSha1());
            if (!files.isEmpty()) {
                media.setCommonsFileNames(files);
                result = true;
            }
        }
        if (media instanceof FullResMedia) {
            FullResMedia<?, ?> frMedia = (FullResMedia<?, ?>) media;
            if (frMedia.getFullResSha1() != null && isEmpty(frMedia.getFullResCommonsFileNames())) {
                Set<String> files = commonsService.findFilesWithSha1(frMedia.getFullResSha1());
                if (!files.isEmpty()) {
                    frMedia.setFullResCommonsFileNames(files);
                    result = true;
                }
            }
        }
        return result;
    }
}
