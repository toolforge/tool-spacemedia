package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.local.Media;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class MediaService {

    @Autowired
    private CommonsService commonsService;

    /**
     * Computes the media SHA-1, if required, and updates the media object accordingly.
     * 
     * @param media    media object
     * @param mediaUrl media URL
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     * @throws URISyntaxException if URL cannot be converted to URI
     */
    public boolean computeSha1(Media media, URL mediaUrl) throws IOException, URISyntaxException {
        if (media.getSha1() == null && mediaUrl != null) {
            media.setSha1(Utils.computeSha1(mediaUrl));
            return true;
        }
        return false;
    }

    /**
     * Looks for Wikimedia Commons files matching the media SHA-1, if required, and
     * updates the media object accordingly.
     * 
     * @param media media object
     * @return {@code true} if media has been updated with list of Wikimedia Commons
     *         files and must be persisted
     */
    public boolean findCommonsFilesWithSha1(Media media) {
        if (media.getSha1() != null && CollectionUtils.isEmpty(media.getCommonsFileNames())) {
            Set<String> files = commonsService.findFilesWithSha1(media.getSha1());
            if (!files.isEmpty()) {
                media.setCommonsFileNames(files);
                return true;
            }
        }
        return false;
    }
}
