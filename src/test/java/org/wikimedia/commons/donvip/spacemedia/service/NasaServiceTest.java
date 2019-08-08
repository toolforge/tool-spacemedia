package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.web.client.RestTemplate;

public class NasaServiceTest {

    @Test
    public void testFindOriginalMedia() throws Exception {
        RestTemplate rest = new RestTemplate();
        for (URL href : Arrays.asList(
                "https://images-assets.nasa.gov/video/EarthKAM_espa%C3%B1ol_V2/collection.json",
                "https://images-assets.nasa.gov/video/EarthKAM_español_V2/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0171_VF_NASA%20KEPLER%20OPENS%20THE%20STUDY%20OF%20THE%20GALAXY%E2%80%99S%20PLANET%20POPULATION/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0171_VF_NASA KEPLER OPENS THE STUDY OF THE GALAXY’S PLANET POPULATION/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0908_Irma%20Tracked%20from%20Space%20on%20This%20Week%20@NASA%20%E2%80%93%20September%208,%202017/collection.json",
                "https://images-assets.nasa.gov/video/NHQ_2017_0908_Irma Tracked from Space on This Week @NASA – September 8, 2017/collection.json"
                ).stream().map(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList())) {
            assertNotNull(NasaService.findOriginalMedia(rest, href));
        }
    }
}
