package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MediaUtilsTest {

    @Test
    @Disabled("Slow")
    void testReadFile() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ContentsAndMetadata<Object> img = MediaUtils.readFile(new URL(
                    "https://capella-open-data.s3.amazonaws.com/data/2023/7/14/CAPELLA_C08_SP_SLC_HH_20230714095227_20230714095245/CAPELLA_C08_SP_SLC_HH_20230714095227_20230714095245.tif"),
                    "tif", null, false, false, httpClient, null);
            assertNotNull(img.ioException());
        }
    }
}
