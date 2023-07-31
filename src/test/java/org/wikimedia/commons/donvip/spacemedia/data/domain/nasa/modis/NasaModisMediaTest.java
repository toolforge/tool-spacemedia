package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;

class NasaModisMediaTest {

    @Test
    void testGetUploadTitle() throws MalformedURLException {
        NasaModisMedia media = new NasaModisMedia();
        FileMetadata m1 = new FileMetadata("http://modis.gsfc.nasa.gov/gallery/images/image12022015_250m.jpg");
        media.addMetadata(m1);
        media.setTitle("Kerguelen Island, South Indian Ocean");

        assertEquals("Kerguelen Island, South Indian Ocean (MODIS)", media.getUploadTitle(m1));
    }
}
