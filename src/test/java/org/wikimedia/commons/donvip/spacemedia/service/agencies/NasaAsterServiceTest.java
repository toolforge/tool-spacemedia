package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterImage;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.NasaAsterService.AsterItem;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests of {@link NasaAsterService}
 */
class NasaAsterServiceTest extends AbstractAgencyServiceTest {

    @Test
    void testJsonDeserialisation() throws Exception {
        AsterItem[] list = new ObjectMapper().readValue(new File("src/test/resources/nasa/aster.json"),
                AsterItem[].class);
        assertEquals(612, list.length);
        assertEquals(
                "AsterItem [lat=-21.0, lng=-68.3, name=Andes, lname=Andes Mts., Chile - Bolivia, cat=Volcanoes, icon=volcanoes]",
                list[0].toString());
    }

    @ParameterizedTest
    @CsvSource({ "sark", "Madrid", "puys" })
    void testParseHtml(String id) throws IOException {
        NasaAsterImage media = new NasaAsterImage();
        NasaAsterService.fillMediaWithHtml(Jsoup.parse(new File("src/test/resources/nasa/aster." + id + ".html")),
                media);
        assertFalse(media.getTitle().isEmpty());
        assertNotNull(media.getThumbnailUrl());
        assertFalse(media.getDescription().isEmpty());
        assertNotNull(media.getDate());
        assertNotNull(media.getMetadata().getSize());
        assertNotNull(media.getDimensions());
        assertNotNull(media.getPublicationDate());
    }
}
