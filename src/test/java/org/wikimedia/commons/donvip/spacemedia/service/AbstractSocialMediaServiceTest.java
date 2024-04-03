package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.ContentsAndMetadata;
import org.wikimedia.commons.donvip.spacemedia.utils.MediaUtils;

@TestPropertySource("/application-test.properties")
class AbstractSocialMediaServiceTest {

    @Test
    void testGetImageUrl_small() throws Exception {
        URL url = AbstractSocialMediaService.getImageUrl(
                CommonsService.getImageUrl(
                        "Transmission_Bands_for_LSST_Filters_(slac-2021_0312_lsst_r_filter_lange-49_5).jpg"),
                848, "Transmission_Bands_for_LSST_Filters_(slac-2021_0312_lsst_r_filter_lange-49_5).jpg");
        assertEquals(
                "https://upload.wikimedia.org/wikipedia/commons/1/17/Transmission_Bands_for_LSST_Filters_(slac-2021_0312_lsst_r_filter_lange-49_5).jpg",
                url.toExternalForm());
        ContentsAndMetadata<BufferedImage> img = MediaUtils.readFile(url, null, false, false);
        assertNotNull(img.contents());
        try {
            assertEquals(86820, img.contentLength());
            assertEquals("jpg", img.extension());
        } finally {
            img.contents().flush();
        }
    }

    @Test
    void testGetImageUrl_large() throws Exception {
        URL url = AbstractSocialMediaService.getImageUrl(
                CommonsService.getImageUrl("LSST_Lens_Cap_Off_(slac-2022_0927_LSST_Lens_Cap_Off_Orrell-46).jpg"), 6503,
                "LSST_Lens_Cap_Off_(slac-2022_0927_LSST_Lens_Cap_Off_Orrell-46).jpg");
        assertEquals(
                "https://commons.wikimedia.org/w/thumb.php?f=LSST_Lens_Cap_Off_(slac-2022_0927_LSST_Lens_Cap_Off_Orrell-46).jpg&w=2560",
                url.toExternalForm());
        ContentsAndMetadata<BufferedImage> img = MediaUtils.readFile(url, null, false, false);
        assertNotNull(img.contents());
        try {
            assertEquals(873339, img.contentLength());
            assertEquals("jpg", img.extension());
        } finally {
            img.contents().flush();
        }
    }
}
