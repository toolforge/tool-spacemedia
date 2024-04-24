package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.wikimedia.commons.donvip.spacemedia.service.orgs.EsaService.getCopernicusProcessedBy;
import static org.wikimedia.commons.donvip.spacemedia.service.orgs.EsaService.isCopyrightOk;

import java.io.File;
import java.net.URL;
import java.util.Optional;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;

@SpringJUnitConfig(EsaServiceTest.TestConfig.class)
class EsaServiceTest extends AbstractOrgServiceTest {

    @Autowired
    private EsaService service;

    @MockBean
    private EsaMediaRepository repository;

    @Test
    void testCopernicusProcessedBy() {
        assertEquals(Optional.empty(), getCopernicusProcessedBy("Contains modified Copernicus Sentinel data (2016)"));
        assertEquals(Optional.empty(), getCopernicusProcessedBy("Contains modified Copernicus Sentinel data [2016]"));

        assertEquals(Optional.of("ESA"), getCopernicusProcessedBy("Copernicus data (2014)/ESA"));
        assertEquals(Optional.of("ESA"), getCopernicusProcessedBy("Copernicus data (2014/2015)/ESA"));
        assertEquals(Optional.of("ESA"), getCopernicusProcessedBy("Copernicus Sentinel data (2015)/ESA"));
        assertEquals(Optional.of("ESA/MyOcean/DMI"),
                getCopernicusProcessedBy("Contains Copernicus data (2014)/ESA/MyOcean/DMI"));

        assertEquals(Optional.of("ESA"),
                getCopernicusProcessedBy("contains modified Copernicus data (2018), processed by ESA"));
        assertEquals(Optional.of("ESA"),
                getCopernicusProcessedBy("contains modified Copernicus Sentinel data (2014-15), processed by ESA"));
        assertEquals(Optional.of("ESA SEOM INSARAP study / InSAR Norway project / NGU / Norut / PPO.labs"),
                getCopernicusProcessedBy(
                        "Contains modified Copernicus Sentinel data (2014–16) / ESA SEOM INSARAP study / InSAR Norway project / NGU / Norut / PPO.labs"));
        assertEquals(Optional.of("ESA"),
                getCopernicusProcessedBy("Contains modified Copernicus Sentinel data (2016)/Processed by ESA"));
        assertEquals(Optional.of("ESA & Sentinel-1 Mission Performance Centre"),
                getCopernicusProcessedBy(
                        "contains modified Copernicus Sentinel data [2016], processed by ESA & Sentinel-1 Mission Performance Centre"));
        assertEquals(Optional.of("ESA"),
                getCopernicusProcessedBy("Contains modified Copernicus Sentinel data [2016]/ processed by ESA"));
    }

    @Test
    void testCopernicusProcessedByGerman() {
        assertEquals(Optional.of("ESA"),
                getCopernicusProcessedBy("Basierend auf von der ESA modifizierten Copernicus Sentinel Daten (2017)"));
        assertEquals(Optional.of("ESA"),
                getCopernicusProcessedBy("Modifizierte und von der ESA bearbeitete Copernicus-Sentinel-Daten (2017)"));
        assertEquals(Optional.of("EUMETSAT"),
                getCopernicusProcessedBy(
                        "Erstellt mit modifizierten Copernicus Sentinel-Daten (2018), bearbeitet von EUMETSAT"));
    }

    @ParameterizedTest
    @CsvSource({
            "false,322928,Friendly_Little_Robot,https://www.esa.int/ESA_Multimedia/Images/2014/09/Friendly_Little_Robot",
            "true,495693,Nuclear_explosions_on_a_neutron_star_feed_its_jets,https://www.esa.int/ESA_Multimedia/Images/2024/03/Nuclear_explosions_on_a_neutron_star_feed_its_jets",
            "true,496653,Barcelona_captured_by_Copernicus_Sentinel-2,https://www.esa.int/ESA_Multimedia/Images/2022/07/Barcelona_captured_by_Copernicus_Sentinel-2" })
    void testParseHtml(boolean copyrightOk, String id, String filename, URL url) throws Exception {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));

        EsaMedia media = new EsaMedia();
        media.setUrl(url);
        service.fillMediaWithHtml(Jsoup.parse(new File("src/test/resources/esa/" + filename + ".htm")), media,
                url);
        assertEquals(id, media.getIdUsedInOrg());
        assertTrue(Integer.parseInt(media.getIdUsedInOrg()) > 0);
        assertTrue(isNotBlank(media.getCredits()));
        assertTrue(isNotBlank(media.getTitle()));
        assertTrue(isNotBlank(media.getDescription()));
        assertTrue(media.getMetadataCount() > 0);
        assertEquals("ESA" + id, media.getUploadId(media.getMetadata().iterator().next()));
        assertEquals(copyrightOk, isCopyrightOk(media), () -> media.getCredits());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public EsaService service(EsaMediaRepository repository) {
            return new EsaService(repository);
        }
    }
}
