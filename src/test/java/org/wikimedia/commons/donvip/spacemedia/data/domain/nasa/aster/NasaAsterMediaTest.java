package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;

class NasaAsterMediaTest {

    @Test
    void testGetUploadTitleOneImage() throws MalformedURLException {
        NasaAsterMedia media = new NasaAsterMedia();
        FileMetadata m1 = new FileMetadata("https://asterweb.jpl.nasa.gov/gallery/images/2017sarknat.jpg");
        media.addMetadata(m1);
        media.setTitle("Sark, English Channel Islands");

        assertEquals("Sark, English Channel Islands (ASTER)", media.getUploadTitle(m1));
    }

    @Test
    void testGetUploadTitleTwoImages() throws MalformedURLException {
        NasaAsterMedia media = new NasaAsterMedia();
        FileMetadata m1 = new FileMetadata("https://asterweb.jpl.nasa.gov/gallery/images/istanbul.jpg");
        FileMetadata m2 = new FileMetadata("https://asterweb.jpl.nasa.gov/gallery/images/istanbul-city.jpg");
        media.addMetadata(m1);
        media.addMetadata(m2);
        media.setTitle("Istanbul");

        assertEquals("istanbul (ASTER)", media.getUploadTitle(m1));
        assertEquals("istanbul-city (ASTER)", media.getUploadTitle(m2));
    }
}
