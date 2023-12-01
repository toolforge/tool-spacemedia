package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMediaRepository;

@SpringJUnitConfig(CopernicusGalleryServiceTest.TestConfig.class)
class CopernicusGalleryServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private CopernicusGalleryMediaRepository repository;

    @Autowired
    private CopernicusGalleryService service;

    private static final Pattern HAS_DAY_MONTH = Pattern.compile(".*" + CopernicusGalleryService.DAY_MONTH + ".*");

    @ParameterizedTest
    @CsvSource({ "smoke-quebec-wildfires-reaches-baltic-sea" })
    void testFillMediaWithHtml(String id) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        CopernicusGalleryMedia media = new CopernicusGalleryMedia();
        service.fillMediaWithHtml(null, Jsoup.parse(new File("src/test/resources/copernicus/" + id + ".html")), media);
        assertEquals("European Union, Copernicus Sentinel-3 imagery", media.getCredits());
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

    @Test
    void testGetLegends() {
        assertEquals(
                "The study was conducted in Portofino, in north-western Italy, shown in this image, which was acquired by one of the Copernicus Sentinel-2 satellites on 18 October 2022.",
                service.getLegends(null, new HashMap<>(Map.of("en",
                "Marine protected areas are an important tool to combat the effects of climate change. In fact, according to study recently published by the CNRS, the French National Centre for Scientific Research, marine protected areas can significantly improve coastal protection and the preservation of the biodiversity of marine organisms, as well as contribute to carbon sequestration, a process that stores carbon dioxide (or other forms of carbon) thus mitigating the accumulation of greenhouse gases in the atmosphere. The study was conducted in Portofino, in north-western Italy, shown in this image, which was acquired by one of the Copernicus Sentinel-2 satellites on 18 October 2022. The data provided by the Copernicus Sentinel satellites and services make it possible to extract key information on the state of implementation of European policies, including those related to the protection of marine areas.")))
                .get("en"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(delimiter = '\t', resources = { "/copernicus/descriptions.csv" })
    void testExtractAcquisitionDate(String id, String description) {
        assumeTrue(HAS_DAY_MONTH.matcher(description).matches());

        CopernicusGalleryMedia media = new CopernicusGalleryMedia();
        media.setId(new CompositeMediaId("copernicus-gallery", id));
        media.setDescription(description);
        media.setPublicationDate(LocalDate.now());

        assertNotNull(service.extractAcquisitionDate(media), description);
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
