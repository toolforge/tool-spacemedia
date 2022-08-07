package org.wikimedia.commons.donvip.spacemedia.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

class UtilsTest {

    /**
     * Test of {@link Utils#readImage}
     */
    @Test
    void testReadWebpImage() throws Exception {
        BufferedImage bi = Utils
                .readImage(new URL("https://upload.wikimedia.org/wikipedia/commons/3/33/2-michel-okujava.webp"),
                true);
        assertNotNull(bi);
        try {
            assertEquals(112, bi.getWidth());
            assertEquals(147, bi.getHeight());
        } finally {
            bi.flush();
        }
    }
}
