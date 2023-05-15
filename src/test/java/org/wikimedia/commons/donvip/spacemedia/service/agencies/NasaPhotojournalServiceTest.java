package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.NasaPhotojournalService.XMLResponseWithoutContentTypeParser;

@SpringJUnitConfig(NasaPhotojournalServiceTest.TestConfig.class)
class NasaPhotojournalServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private NasaPhotojournalMediaRepository repository;

    @Autowired
    private NasaPhotojournalService service;

    @Test
    void testGetLocation() {
        NasaPhotojournalMedia media = new NasaPhotojournalMedia();
        media.setDescription(
                "The image was acquired June 17, 2013, covers an area of 9.2 by 10.2 km, and is located at 57.1 degrees north, 7.3 degrees west.");
        assertEquals(new Point(57.1, -7.3), service.getLocation(media).get());
    }

    @Test
    void testGetCreationDate() {
        NasaPhotojournalMedia media = new NasaPhotojournalMedia();
        media.setDescription(
                "The image was acquired June 17, 2013, covers an area of 9.2 by 10.2 km, and is located at 57.1 degrees north, 7.3 degrees west.");
        assertEquals(LocalDate.of(2013, 6, 17), service.getCreationDate(media).get());
    }

    @Test
    void testReadXml() throws Exception {
        when(mediaService.updateMedia(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any())).thenReturn(new MediaUpdateResult(true, null));
        when(repository.save(any(NasaPhotojournalMedia.class))).thenAnswer(a -> a.getArgument(0, NasaPhotojournalMedia.class));

        List<NasaPhotojournalMedia> medias = service.processResponse(solrResponse("PIA25927"));

        assertEquals(1, medias.size());
        NasaPhotojournalMedia media = medias.get(0);
        assertNotNull(media);
        assertEquals("PIA25927", media.getId());
        assertEquals("JPL-20230509-PIA25927-ODYSSEY", media.getNasaId());
        assertEquals("2023-05-09T13:19:04Z", media.getDate().toString());
        assertEquals("Mars", media.getTarget());
        assertEquals("2001 Mars Odyssey", media.getMission());
        assertEquals("2001 Mars Odyssey", media.getSpacecraft());
        assertEquals("Thermal Emission Imaging System", media.getInstrument());
        assertEquals("Arizona State University", media.getProducer());
        assertEquals(Set.of(), media.getKeywords());
        assertEquals("NASA/JPL-Caltech/ASU", media.getCredit());
        assertFalse(media.isBig());
        assertNotNull(media.getMetadata());
//        assertEquals(3, media.getMetadata().size());
//        assertEquals(List.of("https://photojournal.jpl.nasa.gov/jpeg/PIA25927.jpg",
//                "https://photojournal.jpl.nasa.gov/tiff/PIA25927.tif",
//                "https://photojournal.jpl.nasa.gov/figures/PIA25927_fig1.png"),
//                media.getMetadata().stream().map(m -> m.getAssetUrl().toExternalForm()).toList());
    }

    private static QueryResponse solrResponse(String id) throws IOException {
        try (Reader reader = Files.newBufferedReader(Paths.get("src/test/resources/photojournal", id + ".xml"),
                StandardCharsets.UTF_8)) {
            return new QueryResponse(new XMLResponseWithoutContentTypeParser().processResponse(reader), null);
        }
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaPhotojournalService service(NasaPhotojournalMediaRepository repository) {
            return new NasaPhotojournalService(repository);
        }
    }
}
