package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;

@SpringJUnitConfig(EsaServiceTest.TestConfig.class)
class EsaServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private EsaMediaRepository repository;

    @Test
    void testCopernicusProcessedBy() {
        assertEquals(Optional.empty(), EsaService.getCopernicusProcessedBy("Contains modified Copernicus Sentinel data (2016)"));
        assertEquals(Optional.empty(), EsaService.getCopernicusProcessedBy("Contains modified Copernicus Sentinel data [2016]"));

        assertEquals(Optional.of("ESA"), EsaService.getCopernicusProcessedBy("Copernicus data (2014)/ESA"));
        assertEquals(Optional.of("ESA"), EsaService.getCopernicusProcessedBy("Copernicus data (2014/2015)/ESA"));
        assertEquals(Optional.of("ESA"), EsaService.getCopernicusProcessedBy("Copernicus Sentinel data (2015)/ESA"));
        assertEquals(Optional.of("ESA/MyOcean/DMI"),
                EsaService.getCopernicusProcessedBy("Contains Copernicus data (2014)/ESA/MyOcean/DMI"));

        assertEquals(Optional.of("ESA"),
                EsaService.getCopernicusProcessedBy("contains modified Copernicus data (2018), processed by ESA"));
        assertEquals(Optional.of("ESA"),
                EsaService.getCopernicusProcessedBy("contains modified Copernicus Sentinel data (2014-15), processed by ESA"));
        assertEquals(Optional.of("ESA SEOM INSARAP study / InSAR Norway project / NGU / Norut / PPO.labs"),
                EsaService.getCopernicusProcessedBy("Contains modified Copernicus Sentinel data (2014–16) / ESA SEOM INSARAP study / InSAR Norway project / NGU / Norut / PPO.labs"));
        assertEquals(Optional.of("ESA"),
                EsaService.getCopernicusProcessedBy("Contains modified Copernicus Sentinel data (2016)/Processed by ESA"));
        assertEquals(Optional.of("ESA & Sentinel-1 Mission Performance Centre"),
                EsaService.getCopernicusProcessedBy("contains modified Copernicus Sentinel data [2016], processed by ESA & Sentinel-1 Mission Performance Centre"));
        assertEquals(Optional.of("ESA"),
                EsaService.getCopernicusProcessedBy("Contains modified Copernicus Sentinel data [2016]/ processed by ESA"));
    }

    @Test
    void testCopernicusProcessedByGerman() {
        assertEquals(Optional.of("ESA"),
                EsaService.getCopernicusProcessedBy("Basierend auf von der ESA modifizierten Copernicus Sentinel Daten (2017)"));
        assertEquals(Optional.of("ESA"),
                EsaService.getCopernicusProcessedBy("Modifizierte und von der ESA bearbeitete Copernicus-Sentinel-Daten (2017)"));
        assertEquals(Optional.of("EUMETSAT"),
                EsaService.getCopernicusProcessedBy("Erstellt mit modifizierten Copernicus Sentinel-Daten (2018), bearbeitet von EUMETSAT"));
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
