package org.wikimedia.commons.donvip.spacemedia.service;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImageRepository;

@Service
public class CommonsService {

    @Autowired
    private CommonsImageRepository commonsImageRepository;

    @Autowired
    private CommonsOldImageRepository commonsOldImageRepository;

    public Set<String> findFilesWithSha1(String sha1) {
        // See https://www.mediawiki.org/wiki/Manual:Image_table#img_sha1
        // The SHA-1 hash of the file contents in base 36 format, zero-padded to 31 characters 
        String sha1base36 = String.format("%31s", new BigInteger(sha1, 16).toString(36)).replace(' ', '0');
        Set<String> files = commonsImageRepository.findBySha1(sha1base36).stream().map(CommonsImage::getName).collect(Collectors.toSet());
        if (files.isEmpty()) {
            files.addAll(commonsOldImageRepository.findBySha1(sha1base36).stream().map(CommonsOldImage::getName).collect(Collectors.toSet()));
        }
        return files;
    }
}
