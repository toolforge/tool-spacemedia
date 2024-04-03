package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ImageUtilsTest {

    /**
     * Test of {@link ImageUtils#readImage}
     */
    @ParameterizedTest
    @CsvSource(delimiter = ',', value = {
            "https://upload.wikimedia.org/wikipedia/commons/d/d2/Epichlorhydrin_vzorec.webp,656,334",
            "https://upload.wikimedia.org/wikipedia/commons/7/78/Empty_200x1.jpg,200,1",
            "https://upload.wikimedia.org/wikipedia/commons/e/e2/Artemis_program_%28original_with_wordmark%29.svg,200,185" })
    void testReadImage(URL url, int width, int height) throws Exception {
        BufferedImage bi = (BufferedImage) MediaUtils.readFile(url, true, true).contents();
        assertNotNull(bi);
        try {
            assertEquals(width, bi.getWidth());
            assertEquals(height, bi.getHeight());
        } finally {
            bi.flush();
        }
    }
}
