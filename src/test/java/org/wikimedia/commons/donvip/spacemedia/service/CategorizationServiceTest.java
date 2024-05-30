package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

@SpringJUnitConfig(CategorizationServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class CategorizationServiceTest {

    @Autowired
    private CategorizationService service;

    @Test
    void testCopernicusCredit() {
        for (String credit : Arrays.asList(
                "Contains modified Copernicus Sentinel data [2018-2023], processed by Sentinel Hub/Pierre Markuse",
                "Basierend auf von der ESA modifizierten Copernicus Sentinel Daten (2017), CC BY-SA 3.0 IGO",
                "Contains Copernicus data (2014)/ESA/MyOcean/DMI, CC BY-SA 3.0 IGO",
                "Contains Copernicus data (2015)/ESA/DLR Microwaves and Radar Institute/GFZ/e-GEOS/INGV–ESA SEOM INSARAP study, CC BY-SA 3.0 IGO",
                "Contains Copernicus data (2015)/ESA/IREA-CNR, CC BY-SA 3.0 IGO",
                "Contains Copernicus data (2015)/ESA/Norut/PPO.labs/COMET–ESA SEOM INSARAP study, CC BY-SA 3.0 IGO",
                "Contains Copernicus data (2015)/R. Grandin/IPGP/CNRS, CC BY-SA 3.0 IGO",
                "contains modified Copernicus data (2018), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus data (2019), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2014-15), processed by ESA , CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2014–16) / ESA SEOM INSARAP study / InSAR Norway project / NGU / Norut / PPO.labs, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2014–16), processed by ESA SEOM INSARAP study/PPO.labs /Norut/NGU, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2015), processed by DLR/ESA/Terradue, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2015), processed by Enveo, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015), processed by ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015)/e-GEOS/JRC/EU-EC/ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015)/ENVEO/ESA CCI/FFG, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015)/ESA SEOM INSARAP study PPO.labs/NORUT, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015)/ESA/Array SC Canada , CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015)/ESA/Array SC Canada, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015–16) / ESA SEOM INSARAP study / PPO.labs / Norut / NGU, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2015–16), processed by ESA SEOM INSARAP study/PPO.labs /Norut/SDFE, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015–16), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2015–16)/sarmap/RIICE project/OpenStreetMap contributors (background map), CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2015–16)/TU Wien, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2015–2016), processed by ESA with SNAP, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016) / processed by IWMI, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016) processed by Eumetsat, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016), CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by Brockmann Consult/ Université catholique de Louvain as part of ESA’s Climate Change Initiative Land Cover project, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by DLR/ESA/Terradue and data by OpenStreetMap contributors , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by DLR/ESA/Terradue, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by ESA , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by ESA and CNES, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016), processed by ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016), processed by Geohazards-TEP, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by GeoVille, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by OceanDataLab, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016), processed by VTT Technical Research Centre of Finland Ltd, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016)/CPOM University of Leeds–A. Hogg/University of Edinburgh–N. Gourmelen, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/ESA/CNR-IREA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/ESA/DLR, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/ESA/INGV, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/ESA/Norut, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/HZG, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2016)/Processed by ESA , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016)/processed by ESA/STFC–RAL Space, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016-17), processed by ESA , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016-17), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016-18), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2016–17), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2017), processed by A. Hogg/CPOM/Priestly Centre, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2017), processed by ESA , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2017), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2017), processed by ESA, CC BY-SA 3.0 IGO , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2017), processed by KNMI",
                "contains modified Copernicus Sentinel data (2017), processed by Sinergise/ESA",
                "contains modified Copernicus Sentinel data (2017). Processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2017–18), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2018), processed by ESA (For Landsat image: USGS/ESA), CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2018), processed by ESA , CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2018), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2018), processed by EUMETSAT, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2018–19), processed by ESA, CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data (2019), processed by ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data (2019), processed by ESA; CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data [2015], processed by ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data [2016], CC BY-SA 3.0 IGO",
                "contains modified Copernicus Sentinel data [2016], processed by ESA & Sentinel-1 Mission Performance Centre, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data [2016], processed by ESA, CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data [2016]/ processed by ESA , CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data [2016]/ processed by ESA, CC BY-SA 3.0 IGO",
                "Copernicus data (2014)/ESA", "Copernicus data (2014/2015)/ESA", "Copernicus data (2015)/ESA",
                "Copernicus data/ESA (2014)", "Copernicus data/ESA (2015), CC BY-SA 3.0 IGO",
                "Copernicus Sentinel data (2015)/ESA", "Copernicus Sentinel data (2015)/ESA, CC BY-SA 3.0 IGO",
                "Copernicus Sentinel data (2016)/ESA, CC BY-SA 3.0 IGO",
                "DUE Sentinel-2 for Agriculture project; contains modified Copernicus Sentinel data (2015), CC BY-SA 3.0 IGO",
                "DUE Sentinel-2 for Agriculture project; contains modified Copernicus Sentinel data (2016), CC BY-SA 3.0 IGO",
                "Environment and Climate Change Canada/CIS/contains modified Copernicus Sentinel data (2015), CC BY-SA 3.0 IGO",
                "Erstellt mit modifizierten Copernicus Sentinel-Daten (2018), bearbeitet von EUMETSAT , CC BY-SA 3.0 IGO",
                "IGEO, CSIC, UCM, DARES, Spanish research project RTC-2014-1922-5, Google Earth, contains modified Copernicus Sentinel data (2016), CC BY-SA 3.0 IGO",
                "Modifizierte und von der ESA bearbeitete Copernicus-Sentinel-Daten (2017), CC BY-SA 3.0 IGO",
                "Contains modified Copernicus Sentinel data [2022], processed by <a href=\"https://twitter.com/Pierre_Markuse\">Pierre Markuse</a>",
                "USGS/contains modified Copernicus Sentinel data (2019), processed by ESA, CC BY-SA 3.0 IGO")) {
            assertNotNull(CategorizationService.extractCopernicusTemplate(credit));
        }
    }

    @CsvSource(delimiter = '|', value = { "Photos by JunoCam|P170=Q48546;P1071=Q319;P4082=Q6314027",
            "Photos by WATSON;Self-portrait photos of the Perseverance rover|P170=Q87749354;P1071=Q111;P4082=Q118464949",
            "Satellite pictures|P31=Q125191;P2079=Q725252", "2024 satellite pictures|P31=Q125191;P2079=Q725252",
            "Satellite pictures of 2024 storms|P31=Q125191;P2079=Q725252" })
    @ParameterizedTest
    void testFindCategoriesStatements(String scats, String sprops) {
        for (String cat : scats.split(";")) {
            SdcStatements sdc = new SdcStatements();
            service.findCategoriesStatements(sdc, Set.of(cat));
            for (String prop : sprops.split(";")) {
                String[] kv = prop.split("=");
                assertEquals(kv[1], sdc.get(kv[0]).getKey().toString());
            }
        }
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public CategorizationService service() {
            return new CategorizationService();
        }
    }
}
