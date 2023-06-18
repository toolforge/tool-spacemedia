package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Set;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMediaRepository;

@SpringJUnitConfig(CopernicusGalleryServiceTest.TestConfig.class)
class CopernicusGalleryServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private CopernicusGalleryMediaRepository repository;

    @Autowired
    private CopernicusGalleryService service;

    @ParameterizedTest
    @CsvSource({ "smoke-quebec-wildfires-reaches-baltic-sea" })
    void testFillMediaWithHtml(String id) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        CopernicusGalleryMedia media = new CopernicusGalleryMedia();
        service.fillMediaWithHtml(Jsoup.parse(new File("src/test/resources/copernicus/" + id + ".html")), media);
        assertEquals("European Union, Copernicus Sentinel-3 imagery", media.getCredit());
        assertEquals("Baltic Sea", media.getLocation());
        assertEquals("Smoke from Québec wildfires reaches the Baltic Sea", media.getTitle());
        assertEquals(
                "https://www.copernicus.eu/system/files/styles/image_of_the_day/private/2023-06/image_day/20230616_SmokeInTheBalticSea.jpg?itok=j9Ad-f5C",
                media.getThumbnailUrl().toExternalForm());
        assertEquals(
                "This image, acquired by one of Copernicus’ Sentinel-3 satellites, shows plumes from the Québec wildfires making their way to north-eastern Europe. Canada, which is currently affected by around 450 active fires, has activated the EU Civil Protection Mechanism requesting European assistance. In an immediate response, France, Portugal and Spain sent close to 300 firefighters. Wildfires in Canada have burnt 4.1 million hectares so far, an area as large as the Netherlands. Copernicus data facilitate continuous monitoring of the situation, providing invaluable support to emergency management operations.",
                media.getDescription());
        FileMetadata metadata = media.getUniqueMetadata();
        assertNotNull(metadata);
        assertEquals("https://www.copernicus.eu/system/files/2023-06/image_day/20230616_SmokeInTheBalticSea.jpg", metadata.getAssetUrl().toExternalForm());
        assertEquals(Set.of("Atmosphere", "Fires"), media.getKeywords());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public CopernicusGalleryService service(CopernicusGalleryMediaRepository repository) {
            return new CopernicusGalleryService(repository);
        }
    }
}
