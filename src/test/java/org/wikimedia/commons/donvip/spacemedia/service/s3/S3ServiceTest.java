package org.wikimedia.commons.donvip.spacemedia.service.s3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;

class S3ServiceTest {

    @Test
    void testGetFiles() {
        assertFalse(new S3Service().getFiles(Regions.US_WEST_2, "umbra-open-data-catalog").isEmpty());
    }
}
