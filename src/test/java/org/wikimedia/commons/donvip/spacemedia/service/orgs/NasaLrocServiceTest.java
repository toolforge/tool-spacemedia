package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;

@SpringJUnitConfig(NasaLrocServiceTest.TestConfig.class)
class NasaLrocServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaLrocMediaRepository repository;

    @MockBean
    private NasaMappingService mapping;

    @Autowired
    private NasaLrocService service;

    @ParameterizedTest
    @CsvSource({ "1358,JAXA SLIM Landing,0,[Robotic Spacecraft],3" })
    void testFillMediaWithHtml(String id, String title, int descLen, String tags, int nFiles) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NasaLrocMedia media = new NasaLrocMedia();
        service.fillMediaWithHtml(null, Jsoup.parse(new File("src/test/resources/nasa/lroc/" + id + ".htm")), media);
        assertEquals(title, media.getTitle());
        assertEquals(tags, media.getKeywords().toString());
        assertEquals(descLen, Optional.ofNullable(media.getDescription()).orElse("").length());
        assertEquals(nFiles, media.getMetadataCount());
        for (FileMetadata fm : media.getMetadata()) {
            assertFalse(fm.getDescription().isEmpty());
        }
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaLrocService service(NasaLrocMediaRepository repository) {
            return new NasaLrocService(repository);
        }
    }
}
