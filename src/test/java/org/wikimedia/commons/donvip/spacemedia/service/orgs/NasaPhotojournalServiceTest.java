package org.wikimedia.commons.donvip.spacemedia.service.orgs;

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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.NasaPhotojournalService.XMLResponseWithoutContentTypeParser;

@SpringJUnitConfig(NasaPhotojournalServiceTest.TestConfig.class)
class NasaPhotojournalServiceTest extends AbstractOrgServiceTest {

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
        NasaPhotojournalService.detectCreationDate(media);
        assertEquals(LocalDate.of(2013, 6, 17), service.getCreationDate(media).get());
    }

    @Test
    void testReadXml1() throws Exception {
        NasaPhotojournalMedia media = readXml("PIA25927");

        assertEquals("JPL-20230509-PIA25927-ODYSSEY", media.getNasaId());
        assertEquals("2023-05-09T13:19:04Z", media.getPublicationDateTime().toString());
        assertEquals("Mars", media.getTarget());
        assertEquals("2001 Mars Odyssey", media.getMission());
        assertEquals("2001 Mars Odyssey", media.getSpacecraft());
        assertEquals("Thermal Emission Imaging System", media.getInstrument());
        assertEquals("Arizona State University", media.getProducer());
        assertEquals(Set.of(), media.getKeywords());
        assertEquals("NASA/JPL-Caltech/ASU", media.getCredits());
        assertFalse(media.isBig());
        assertNotNull(media.getMetadata());
        assertEquals(3, media.getMetadata().size());
        assertEquals(List.of("https://photojournal.jpl.nasa.gov/jpeg/PIA25927.jpg",
                "https://photojournal.jpl.nasa.gov/tiff/PIA25927.tif",
                "https://photojournal.jpl.nasa.gov/figures/PIA25927_fig1.png"),
                media.getMetadata().stream().map(m -> m.getAssetUrl().toExternalForm()).toList());

        FileMetadata jpeg = media.getMetadata().iterator().next();
        assertEquals(
                Optional.of("Kasei Valles (PIA25927).tiff|TIFF version\nKasei Valles (PIA25927_fig1).png|PNG version"),
                service.getOtherVersions(media, jpeg));
        assertEquals(611, jpeg.getImageDimensions().getWidth());
        assertEquals(2696, jpeg.getImageDimensions().getHeight());

        Map<String, Pair<Object, Map<String, Object>>> statements = service.getStatements(media, jpeg);
        assertEquals(Pair.of("Q1108979", null), statements.get("P4082"));
        assertEquals(Pair.of("Q207164", null), statements.get("P170"));

        assertEquals(
                "<p><center>[[File:Kasei Valles (PIA25927_fig1).png|120px]]<br><b>Context image</b></center></p><p>Today's VIS image shows a portion of Kasei Valles. Kasei Valles is one of the largest outflow channel systems on Mars, in places up to 482 km (300 miles) wide and 1580 km (982 miles) long. For comparison, the Grand Canyon in Arizona is is only 29 km (18 miles) at its widest and only 446 km (277 miles) long. Kasei Valles flows eastward through Lunae Planum and empties into Chryse Planitia.</p><p>Orbit Number: 93928 Latitude: 25.9137 Longitude: 289.462 Instrument: VIS Captured: 2023-02-16 08:13</p><p>Please see the <a href=\"https://themis.asu.edu/terms\" target=\"_new\">THEMIS Data Citation Note</a> for details on crediting THEMIS images.</p><p>NASA's Jet Propulsion Laboratory manages the 2001 Mars Odyssey mission for NASA's Science Mission Directorate, Washington, D.C. The Thermal Emission Imaging System (THEMIS) was developed by Arizona State University, Tempe, in collaboration with Raytheon Santa Barbara Remote Sensing. The THEMIS investigation is led by Dr. Philip Christensen at Arizona State University. Lockheed Martin Astronautics, Denver, is the prime contractor for the Odyssey project, and developed and built the orbiter. Mission operations are conducted jointly from Lockheed Martin and from JPL, a division of the California Institute of Technology in Pasadena.</p>",
                service.getDescription(media, jpeg));
    }

    @Test
    void testReadXml2() throws Exception {
        NasaPhotojournalMedia media = readXml("PIA25889");

        assertEquals(7, media.getMetadata().size());
        assertEquals(
                List.of("https://photojournal.jpl.nasa.gov/jpeg/PIA25889.jpg",
                        "https://photojournal.jpl.nasa.gov/tiff/PIA25889.tif",
                        "https://photojournal.jpl.nasa.gov/figures/PIA25889_figA.jpg",
                        "https://photojournal.jpl.nasa.gov/figures/PIA25889_figB.jpg",
                        "https://photojournal.jpl.nasa.gov/archive/PIA25889_MAIN_fullres.jpg",
                        "https://photojournal.jpl.nasa.gov/archive/PIA25889_FIGA_fullres.jpg",
                        "https://photojournal.jpl.nasa.gov/archive/PIA25889_FIGB_fullres.jpg"),
                media.getMetadata().stream().map(m -> m.getAssetUrl().toExternalForm()).toList());

        assertEquals(List.of("PIA25889", "PIA25889", "PIA25889_figA", "PIA25889_figB", "PIA25889", "PIA25889_figA",
                "PIA25889_figB"), media.getMetadata().stream().map(m -> media.getUploadId(m)).toList());
    }

    private NasaPhotojournalMedia readXml(String id) throws IOException, UploadException {
        when(mediaService.updateMedia(any(), any(), anyBoolean(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any())).thenReturn(new MediaUpdateResult(true, null));
        when(repository.save(any(NasaPhotojournalMedia.class))).thenAnswer(a -> a.getArgument(0, NasaPhotojournalMedia.class));
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));

        List<NasaPhotojournalMedia> medias = service.processResponse(solrResponse(id));

        assertEquals(1, medias.size());
        NasaPhotojournalMedia media = medias.get(0);
        assertNotNull(media);
        assertEquals(id, media.getId().getMediaId());
        return media;
    }

    private static QueryResponse solrResponse(String id) throws IOException {
        try (Reader reader = Files.newBufferedReader(Paths.get("src/test/resources/photojournal", id + ".xml"),
                StandardCharsets.UTF_8)) {
            return new QueryResponse(new XMLResponseWithoutContentTypeParser().processResponse(reader), null);
        }
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        public NasaMappingService mappings() {
            return new NasaMappingService();
        }

        @Bean
        @Autowired
        public NasaPhotojournalService service(NasaPhotojournalMediaRepository repository) {
            return new NasaPhotojournalService(repository);
        }
    }
}
