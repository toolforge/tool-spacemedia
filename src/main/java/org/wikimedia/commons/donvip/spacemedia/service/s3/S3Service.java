package org.wikimedia.commons.donvip.spacemedia.service.s3;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Service
public class S3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Service.class);

    private static final Set<String> OK_EXT = Set.of("wav", "mp3", "flac", "midi", "bmp", "jpg", "jpeg", "tif", "tiff",
            "pdf", "png", "webp", "xcf", "gif", "svg", "mp4", "webm", "ogv", "mpeg");

    public List<S3ObjectSummary> getFiles(Regions region, String bucket) {
        LOGGER.info("Looking for S3 media in {}...", bucket);
        return AmazonS3ClientBuilder.standard().withRegion(region).build().listObjects(bucket)
                .getObjectSummaries().stream()
                .filter(x -> OK_EXT.contains(x.getKey().substring(x.getKey().lastIndexOf('.') + 1))).toList();
    }
}
