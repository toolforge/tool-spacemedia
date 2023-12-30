package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.threeten.bp.LocalDate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.UploadMode;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.GeometryService;
import org.wikimedia.commons.donvip.spacemedia.service.InternetArchiveService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.NasaAsterService.AsterItem;

/**
 * Unit tests of {@link NasaAsterService}
 */
@SpringJUnitConfig(classes = NasaAsterServiceTest.TestConfig.class)
class NasaAsterServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaAsterMediaRepository repository;

    @MockBean
    private GeometryService geometryService;

    @Autowired
    private NasaAsterService service;

    @Test
    void testJsonDeserialisation() throws Exception {
        AsterItem[] list = json("nasa/aster/gallery.json", AsterItem[].class);
        assertEquals(612, list.length);
        assertEquals(
                "AsterItem[lat=-21.0, lng=-68.3, name=Andes, lname=Andes Mts., Chile - Bolivia, cat=Volcanoes, icon=volcanoes]",
                list[0].toString());
    }

    @ParameterizedTest
    @CsvSource({ "sark", "Madrid", "puys", "cumbrevieja", "hurricaneike", "EtnaSO2", "pakistanlake", "sangay2",
            "Hayman", "chernobyl", "Raikoke", "etnaeruption", "Sydney", "antikythera", "Singapore", "kizimen",
            "uzbekistan", "Eyjafyallajokull", "gdem", "surfire", "Anatahan", "zaca", "tibesti", "monument", "horn",
            "topanga", "msh", "aceh", "Hawaii", "Isabel", "AspenFire", "Vegas" })
    void testParseHtmlOneImage(String id) throws Exception {
        basicChecks(id, 1);
    }

    @ParameterizedTest
    @CsvSource({ "Istanbul", "oroville", "beihai", "falconreservoir", "cancun", "frisco", "NZglaciers", "saltonsea",
            "mead", "sanberdofire" })
    void testParseHtmlTwoImages(String id) throws Exception {
        basicChecks(id, 2);
    }

    @ParameterizedTest
    @CsvSource({ "tokyo-snow" })
    void testFromInternetArchive(String id) throws Exception {
        basicChecks(id, 1,
                "https://web.archive.org/web/20060824092636/http://asterweb.jpl.nasa.gov:80/gallery-detail.asp?name=tokyo-snow",
                false);
    }

    private NasaAsterMedia basicChecks(String id, int size) throws MalformedURLException, IOException {
        return basicChecks(id, size, "https://asterweb.jpl.nasa.gov/gallery-detail.asp?name=" + id, true);
    }

    private NasaAsterMedia basicChecks(String id, int size, String url, boolean sizeAndDims)
            throws MalformedURLException, IOException {
        NasaAsterMedia media = new NasaAsterMedia();
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        when(commonsService.isPermittedFileUrl(any())).thenReturn(true);

        service.fillMediaWithHtml(Jsoup.parse(new File("src/test/resources/nasa/aster/" + id + ".html")), media, url);

        assertFalse(media.getTitle().isEmpty());
        assertNotNull(media.getThumbnailUrl());
        assertFalse(media.getDescription().isEmpty());
        assertNotNull(media.getCreationDate());
        assertNotNull(media.getPublicationDateTime());
        assertTrue(media.getYear().getValue() >= 1999);
        assertTrue(media.getYear().getValue() <= LocalDate.now().getYear());
        assertTrue(media.getYear().getValue() <= media.getPublicationDateTime().getYear());
        assertEquals(size, media.getMetadata().size());

        Set<String> uploadTitles = new TreeSet<>();
        for (FileMetadata metadata : media.getMetadata()) {
            assertNotNull(metadata);
            if (sizeAndDims) {
                assertNotNull(metadata.getSize());
                assertNotNull(metadata.getImageDimensions());
            }
            UploadContext<NasaAsterMedia> uploadContext = new UploadContext<>(media, metadata, UploadMode.AUTO, 1999,
                    service::isPermittedFileType, false);
            assertTrue(uploadContext.shouldUpload());
            assertTrue(uploadContext.shouldUploadAuto());
            String uploadTitle = media.getUploadTitle(metadata);
            assertNotNull(uploadTitle);
            assertTrue(uploadTitles.add(uploadTitle));
            Optional<String> otherVersions = service.getOtherVersions(media, metadata);
            assertTrue(size > 1 ? otherVersions.isPresent() : otherVersions.isEmpty());
        }
        assertEquals(size, uploadTitles.size());

        return media;
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        public InternetArchiveService internetArchive() {
            return new InternetArchiveService();
        }

        @Bean
        @Autowired
        public NasaAsterService service(NasaAsterMediaRepository repository) {
            return new NasaAsterService(repository);
        }
    }
}
