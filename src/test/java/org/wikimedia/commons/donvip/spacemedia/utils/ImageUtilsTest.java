package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ImageUtilsTest {

    /**
     * Test of {@link ImageUtils#readImage}
     */
    @ParameterizedTest
    @CsvSource(delimiter = ',', value = {
            "https://upload.wikimedia.org/wikipedia/commons/3/33/2-michel-okujava.webp,112,147",
            "https://upload.wikimedia.org/wikipedia/commons/7/78/Empty_200x1.jpg,200,1",
            "https://upload.wikimedia.org/wikipedia/commons/e/e2/Artemis_program_%28original_with_wordmark%29.svg,200,185" })
    void testReadImage(URL url, int width, int height) throws Exception {
        BufferedImage bi = ImageUtils.readImage(url, true, true).getLeft();
        assertNotNull(bi);
        try {
            assertEquals(width, bi.getWidth());
            assertEquals(height, bi.getHeight());
        } finally {
            bi.flush();
        }
    }

    @Test
    void testReadImageMetadata() throws Exception {
        // ImageUtils.readImageMetadata(new
        // URL("https://images-assets.nasa.gov/image/P22-003-22/P22-003-22~orig.jpg"));
        // TODO
    }
}
