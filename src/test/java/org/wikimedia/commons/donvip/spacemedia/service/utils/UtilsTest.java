package org.wikimedia.commons.donvip.spacemedia.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

class UtilsTest {

    /**
     * Test of {@link Utils#readImage} with WebP
     */
    @Test
    void testReadWebpImage() throws Exception {
        BufferedImage bi = Utils
                .readImage(new URL("https://upload.wikimedia.org/wikipedia/commons/3/33/2-michel-okujava.webp"), true,
                        true);
        assertNotNull(bi);
        try {
            assertEquals(112, bi.getWidth());
            assertEquals(147, bi.getHeight());
        } finally {
            bi.flush();
        }
    }

    /**
     * Test of {@link Utils#readImage} with Jpeg
     */
    @Test
    void testReadJpgImage() throws Exception {
        BufferedImage bi = Utils.readImage(
                new URL("https://upload.wikimedia.org/wikipedia/commons/7/74/Pile_Load_Testing.jpg"), true, true);
        assertNotNull(bi);
        try {
            assertEquals(1, bi.getWidth());
            assertEquals(1, bi.getHeight());
        } finally {
            bi.flush();
        }
    }
}
