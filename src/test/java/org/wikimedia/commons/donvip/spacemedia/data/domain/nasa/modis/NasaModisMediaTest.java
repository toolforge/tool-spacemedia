package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.time.LocalDate;

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

    @Test
    void testCopyDataFrom() {
        NasaModisMedia m1 = new NasaModisMedia();
        m1.setSatellite("foo");
        m1.setBands("bar");
        m1.setCredits("baz");
        m1.setPublicationDate(LocalDate.now());

        NasaModisMedia m2 = new NasaModisMedia().copyDataFrom(m1);

        assertEquals(m1.getSatellite(), m2.getSatellite());
        assertEquals(m1.getBands(), m2.getBands());
        assertEquals(m1.getCredits(), m2.getCredits());
        assertEquals(m1.getPublicationDate(), m2.getPublicationDate());
    }
}
