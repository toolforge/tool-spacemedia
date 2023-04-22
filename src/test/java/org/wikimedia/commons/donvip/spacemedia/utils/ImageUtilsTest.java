package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.net.URL;

import org.junit.jupiter.api.Test;

class ImageUtilsTest {

    /**
     * Test of {@link ImageUtils#readImage} with WebP
     */
    @Test
    void testReadWebpImage() throws Exception {
        BufferedImage bi = ImageUtils
                .readImage(new URL("https://upload.wikimedia.org/wikipedia/commons/3/33/2-michel-okujava.webp"), true,
                        true)
                .getLeft();
        assertNotNull(bi);
        try {
            assertEquals(112, bi.getWidth());
            assertEquals(147, bi.getHeight());
        } finally {
            bi.flush();
        }
    }

    /**
     * Test of {@link ImageUtils#readImage} with Jpeg
     */
    @Test
    void testReadJpgImage() throws Exception {
        BufferedImage bi = ImageUtils.readImage(
                new URL("https://upload.wikimedia.org/wikipedia/commons/7/78/Empty_200x1.jpg"), true, true)
                .getLeft();
        assertNotNull(bi);
        try {
            assertEquals(200, bi.getWidth());
            assertEquals(1, bi.getHeight());
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
