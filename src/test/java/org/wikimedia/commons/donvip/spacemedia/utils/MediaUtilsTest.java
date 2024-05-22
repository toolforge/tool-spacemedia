package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MediaUtilsTest {

    @Test
    @Disabled("Slow")
    void testReadFile() throws Exception {
        ContentsAndMetadata<Object> img = MediaUtils.readFile(new URL(
                "https://capella-open-data.s3.amazonaws.com/data/2023/7/14/CAPELLA_C08_SP_SLC_HH_20230714095227_20230714095245/CAPELLA_C08_SP_SLC_HH_20230714095227_20230714095245.tif"),
                "tif", null, false, false);
        assertNotNull(img.ioException());
    }
}
