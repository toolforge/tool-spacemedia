package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis.NasaModisMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis.NasaModisMediaRepository;

/**
 * Unit tests of {@link NasaModisService}
 */
@SpringJUnitConfig(classes = NasaModisServiceTest.TestConfig.class)
class NasaModisServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaModisMediaRepository repository;

    @Autowired
    private NasaModisService service;

    @ParameterizedTest
    @CsvSource({ "2015-12-02", "2022-05-07", "2023-06-30", "2023-07-03" })
    void testParseHtml(LocalDate date) throws Exception {
        basicChecks(date);
    }

    private NasaModisMedia basicChecks(LocalDate date) throws Exception {
        NasaModisMedia media = new NasaModisMedia();
        media.setPublicationDate(date);
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        when(commonsService.isPermittedFileUrl(any())).thenReturn(true);

        service.fillMediaWithHtml(Jsoup.parse(new File("src/test/resources/nasa/modis/" + date + ".htm")), media);

        assertFalse(media.getCredit().isEmpty());
        assertFalse(media.getTitle().isEmpty());
        assertNotNull(media.getThumbnailUrl());
        assertFalse(media.getDescription().isEmpty());
        assertNotNull(media.getDate());
        assertNotNull(media.getPublicationDate());
        assertTrue(media.getYear().getValue() >= 1999);
        assertTrue(media.getYear().getValue() <= LocalDate.now().getYear());
        assertTrue(media.getYear().getValue() <= media.getPublicationDate().getYear());
        assertTrue(List.of("Aqua", "Terra").contains(media.getSatellite()));

        return media;
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaModisService service(NasaModisMediaRepository repository) {
            return new NasaModisService(repository);
        }
    }
}
