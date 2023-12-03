package org.wikimedia.commons.donvip.spacemedia.service.s3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Comparator;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3ObjectSummary;

class S3ServiceTest {

    @Test
    void testGetFiles() {
        assertFalse(
                new S3Service().getFiles(Regions.US_WEST_2, "umbra-open-data-catalog", Function.identity(),
                        f -> true, Comparator.comparing(S3ObjectSummary::getKey)).isEmpty());
    }
}
