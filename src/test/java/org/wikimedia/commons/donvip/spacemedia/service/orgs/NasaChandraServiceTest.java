package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra.NasaChandraMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra.NasaChandraMediaRepository;

@SpringJUnitConfig(NasaChandraServiceTest.TestConfig.class)
class NasaChandraServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaChandraMediaRepository repository;

    @Autowired
    private NasaChandraService service;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "1999/casajph;Cassiopeia A: Chandra Maps Vital Elements in Supernovas;547;3",
            "2023/ngc2264;NGC 2264: Sprightly Stars Illuminate 'Christmas Tree Cluster';4865;3"
    })
    void testFillMediaWithHtml(String id, String title, int descLen, int nFiles)
            throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NasaChandraMedia media = new NasaChandraMedia();
        media.setId(new CompositeMediaId("chandra", id));
        service.fillMediaWithHtml("https://chandra.si.edu/photo/" + id,
                Jsoup.parse(new File("src/test/resources/nasa/chandra/" + id.replace('/', '_') + ".htm")),
                media);
        assertEquals(title, media.getTitle());
        assertEquals(descLen, Optional.ofNullable(media.getDescription()).orElse("").length());
        assertEquals(nFiles, media.getMetadataCount());
        assertNotNull(media.getCredits());
        assertNotNull(media.getPublicationDate());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaChandraService service(NasaChandraMediaRepository repository) {
            return new NasaChandraService(repository);
        }
    }
}
