package org.wikimedia.commons.donvip.spacemedia.service.nasa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringJUnitConfig(NasaMediaProcessorServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class NasaMediaProcessorServiceTest {

    @MockBean
    private ExifMetadataRepository exifRepo;
    @MockBean
    private NasaMediaRepository<?> nasaRepo;
    @MockBean
    private MediaService mediaService;

    @Autowired
    private NasaMediaProcessorService service;

    @Test
    void testFindOriginalMedia() throws Exception {
        RestTemplate rest = new RestTemplate();
        for (URL href : Arrays.asList("https://images-assets.nasa.gov/video/EarthKAM_espa%C3%B1ol_V2/collection.json",
                "https://images-assets.nasa.gov/video/EarthKAM_español_V2/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0171_VF_NASA%20KEPLER%20OPENS%20THE%20STUDY%20OF%20THE%20GALAXY%E2%80%99S%20PLANET%20POPULATION/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0171_VF_NASA KEPLER OPENS THE STUDY OF THE GALAXY’S PLANET POPULATION/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0908_Irma%20Tracked%20from%20Space%20on%20This%20Week%20@NASA%20%E2%80%93%20September%208,%202017/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0908_Irma Tracked from Space on This Week @NASA – September 8, 2017/collection.json")
                .stream().map(s -> {
                    try {
                        return new URL(s);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).toList()) {
            assertNotNull(NasaMediaProcessorService.findOriginalMedia(rest, href));
        }
    }

    @ParameterizedTest
    @CsvSource({ "AFRC2021-0128-85", "KSC-20220714-PH-KMO06_0021" })
    void testExifRestTemplate(String id) throws Exception {
        assertNotNull((service.readExifMetadata(service.restExifTemplate(), id)).getIptcByLine());
    }

    @Test
    void testKeywordsSplit() throws Exception {
        doTestKeywords("MSFC; National Space Advisory Council; U.S. Space and Rocket Cen",
                Arrays.asList("MSFC", "National Space Advisory Council", "U.S. Space and Rocket Cen"));
        doTestKeywords("Human Exploration Rover Challenge; U.S. Space and Rocket Center;",
                Arrays.asList("Human Exploration Rover Challenge", "U.S. Space and Rocket Center"));
        doTestKeywords(
                "NASA,Jet Propulsion Laboratory,JPL,space,exploration,planets,InSight,lander,Interior Exploration using Seismic Investigations,Geodesy and Heat Transport,Martian wind,Marsforming,AR,instrument deployment,SEIS,Seismic Experiment for Interior Structure,Curiosity,Mars Science Laboratory,MSL,science,Mars,planet,news,robot,robotics,tech,technology,augmented reality,hololens",
                Arrays.asList("NASA", "Jet Propulsion Laboratory", "JPL", "space", "exploration", "planets", "InSight",
                        "lander", "Interior Exploration using Seismic Investigations", "Geodesy and Heat Transport",
                        "Martian wind", "Marsforming", "AR", "instrument deployment", "SEIS",
                        "Seismic Experiment for Interior Structure", "Curiosity", "Mars Science Laboratory", "MSL",
                        "science", "Mars", "planet", "news", "robot", "robotics", "tech", "technology",
                        "augmented reality", "hololens"));
        doTestKeywords("Chandra X-ray Observatory,NuSTAR", Arrays.asList("Chandra X-ray Observatory", "NuSTAR"));
        doTestKeywords("iss,", Arrays.asList("iss"));
        doTestKeywords(
                "NASA, JPL, Jet Propulsion Laboratory, GRACE, GRACE Follow-On, Gravity Recovery and Climate Experiment, water, water cycle, launch, Falcon 9, SpaceX, rocket, GFZ, German Research Centre for Geosciences, gravity, measurements, sea level rise, glaciers, ice sheets, Greenland, Antarctica, melting, aquifer, groundwater, soil moisture, droughts, floods, lakes, rivers, climate change, Vandenberg Air Force Base, VAFB, movement, mass changes, weather forecasting, microwave instrument, laser ranging interferometer",
                Arrays.asList("NASA", "JPL", "Jet Propulsion Laboratory", "GRACE", "GRACE Follow-On",
                        "Gravity Recovery and Climate Experiment", "water", "water cycle", "launch", "Falcon 9",
                        "SpaceX", "rocket", "GFZ", "German Research Centre for Geosciences", "gravity", "measurements",
                        "sea level rise", "glaciers", "ice sheets", "Greenland", "Antarctica", "melting", "aquifer",
                        "groundwater", "soil moisture", "droughts", "floods", "lakes", "rivers", "climate change",
                        "Vandenberg Air Force Base", "VAFB", "movement", "mass changes", "weather forecasting",
                        "microwave instrument", "laser ranging interferometer"));
    }

    @Test
    void testKeywordsNoSplitDates() throws Exception {
        doTestKeywords("USA Composite Reveals Massive Winter Storm - January 02, 2014",
                Arrays.asList("USA Composite Reveals Massive Winter Storm - January 02, 2014"));
        doTestKeywords("Erupting Prominence Observed by SDO on March 30, 2010",
                Arrays.asList("Erupting Prominence Observed by SDO on March 30, 2010"));
        doTestKeywords("C3-class Solar Flare Erupts on Sept. 8, 2010 [Detail]",
                Arrays.asList("C3-class Solar Flare Erupts on Sept. 8, 2010 [Detail]"));
    }

    @Test
    void testKeywordsNoSplitVariousStuff() throws Exception {
        doTestKeywords("Kulusuk Icebergs, by Andrew Bossi", Arrays.asList("Kulusuk Icebergs, by Andrew Bossi"));
        doTestKeywords("Hi, Hokusai!", Arrays.asList("Hi, Hokusai!"));
        doTestKeywords("U.S. Senate Committee on Commerce, Science and Transportation",
                Arrays.asList("U.S. Senate Committee on Commerce, Science and Transportation"));
        doTestKeywords("Entry, Descent and Landing (EDL)", Arrays.asList("Entry, Descent and Landing (EDL)"));
        doTestKeywords("NASA's SDO Satellite Captures Venus Transit Approach -- Bigger, Better!",
                Arrays.asList("NASA's SDO Satellite Captures Venus Transit Approach -- Bigger, Better!"));
    }

    @Test
    void testKeywordsNoSplitContinentsCountriesStates() throws Exception {
        doTestKeywords("Partial Eclipse Seen Over the Princess Ragnhild Coast, Antarctica",
                Arrays.asList("Partial Eclipse Seen Over the Princess Ragnhild Coast, Antarctica"));
        doTestKeywords("Eastern Hudson Bay, Canada", Arrays.asList("Eastern Hudson Bay, Canada"));
        doTestKeywords("Landsat View: Western Suburbs of Chicago, Illinois",
                Arrays.asList("Landsat View: Western Suburbs of Chicago, Illinois"));
        doTestKeywords("Satellite Sees Holiday Lights Brighten Cities - Washington, D.C., and Baltimore",
                Arrays.asList("Satellite Sees Holiday Lights Brighten Cities - Washington, D.C., and Baltimore"));
        doTestKeywords("Smoke from Fires in Southwestern Oregon, Northern California",
                Arrays.asList("Smoke from Fires in Southwestern Oregon, Northern California"));
        doTestKeywords("Worcester, MA", Arrays.asList("Worcester, MA"));
        doTestKeywords("Washington, DC", Arrays.asList("Washington, DC"));
    }

    @Test
    void testKeywordsNoSplitNumbers() throws Exception {
        doTestKeywords("Hubble views a spectacular supernova with interstellar material over 160,000 light-years away",
                Arrays.asList(
                        "Hubble views a spectacular supernova with interstellar material over 160,000 light-years away"));
    }

    private static void doTestKeywords(String string, List<String> asList) {
        assertEquals(new HashSet<>(asList), NasaMediaProcessorService.normalizeKeywords(Collections.singleton(string)));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule());
        }

        @Bean
        public NasaMediaProcessorService service() {
            return new NasaMediaProcessorService();
        }
    }
}
