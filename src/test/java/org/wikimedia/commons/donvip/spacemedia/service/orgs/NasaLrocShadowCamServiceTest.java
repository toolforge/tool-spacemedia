package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.IgnoreException;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;

@SpringJUnitConfig(NasaLrocShadowCamServiceTest.TestConfig.class)
class NasaLrocShadowCamServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaLrocMediaRepository repository;

    @MockBean
    private NasaMappingService mapping;

    @Autowired
    private NasaLrocShadowCamService service;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "lroc;157;LROC’s First Look at the Apollo Landing Sites;0;[Apollo, Robotic Spacecraft];15",
            "lroc;1321;IM-1 Landing Region;0;[Robotic Spacecraft];3",
            "shadowcam;1357;Complicated Lighting;0;[Permanently Shadowed Regions];4",
            "lroc;1358;JAXA SLIM Landing;0;[Robotic Spacecraft];3" })
    void testFillMediaWithHtml(String repoId, String id, String title, int descLen, String tags, int nFiles) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NasaLrocMedia media = new NasaLrocMedia();
        media.setId(new CompositeMediaId(repoId, id));
        service.fillMediaWithHtml(null, Jsoup.parse(new File("src/test/resources/nasa/lroc-shadowcam/" + id + ".htm")), media);
        assertEquals(title, media.getTitle());
        assertEquals(tags, media.getKeywords().toString());
        assertEquals(descLen, Optional.ofNullable(media.getDescription()).orElse("").length());
        assertEquals(nFiles, media.getMetadataCount());
        for (FileMetadata fm : media.getMetadata()) {
            if (!fm.getAssetUri().toString().contains("/ptif/download_file?fName=")) {
                assertFalse(fm.getDescription().isEmpty(), () -> fm.toString());
            }
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "Alan Shepard's view of space from Freedom 7 [NASA/JSC/Arizona State University]!;NASA/JSC/Arizona State University;false",
            "Full size replica of Mercury Redstone rocket, with Mercury capsule, sitting on the pad at Launch Complex 5/6 at Cape Canaveral. Imagine being strapped in that tiny capsule (the base is only six feet in diameter) awaiting launch [Photo: Mark Robinson].;Photo: Mark Robinson;true" })
    void testCredits(String description, String credit, boolean throwError) {
        FileMetadata metadata = new FileMetadata();
        metadata.setDescription(description);
        NasaLrocMedia media = new NasaLrocMedia();
        media.setId(new CompositeMediaId("lroc", "foo"));
        if (throwError) {
            assertEquals("Non-NASA picture: Photo: Mark Robinson",
                    assertThrows(IgnoreException.class, () -> service.getAuthor(media, metadata)).getMessage());
        } else {
            assertEquals(credit, service.getAuthor(media, metadata));
        }
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaLrocShadowCamService service(NasaLrocMediaRepository repository) {
            return new NasaLrocShadowCamService(repository);
        }
    }
}
