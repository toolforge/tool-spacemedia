package org.wikimedia.commons.donvip.spacemedia.service.s3;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Service
public class S3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Service.class);

    private static final Set<String> OK_EXT = Set.of("wav", "mp3", "flac", "midi", "bmp", "jpg", "jpeg", "tif", "tiff",
            "pdf", "png", "webp", "xcf", "gif", "svg", "mp4", "webm", "ogv", "mpeg");

    static {
        System.setProperty(SDKGlobalConfiguration.AWS_EC2_METADATA_DISABLED_SYSTEM_PROPERTY, "true");
    }

    public <T> List<T> getFiles(Regions region, String bucket, Function<S3ObjectSummary, T> mapper,
            Comparator<T> comparator) {
        LOGGER.info("Looking for S3 media in {}...", bucket);
        return AmazonS3ClientBuilder.standard().withRegion(region).build().listObjects(bucket)
                .getObjectSummaries().stream()
                .filter(x -> OK_EXT.contains(x.getKey().substring(x.getKey().lastIndexOf('.') + 1))).map(mapper)
                .sorted(comparator.reversed())
                .toList();
    }

    public S3Object getObject(Regions region, String bucket, String key) {
        return AmazonS3ClientBuilder.standard().withRegion(region).build().getObject(bucket, key);
    }
}