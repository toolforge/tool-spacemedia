package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaType;

@SpringJUnitConfig(WebbEsaServiceTest.TestConfig.class)
class WebbEsaServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private DjangoplicityMediaRepository repository;

    @Autowired
    private WebbEsaService service;

    @Test
    void testReadHtml() throws Exception {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        DjangoplicityMedia media = service.newMediaFromHtml(html("esawebb/WLMb.html"),
                new URL("https://esawebb.org/images/WLMb/"), "WLMb", null);
        assertNotNull(media);
        assertEquals("WLMb", media.getId().getId());
        assertEquals("Dwarf Galaxy WLM", media.getTitle());
        assertEquals(
                "This image shows a portion of the dwarf galaxy Wolf–Lundmark–Melotte (WLM) captured by the NASA/ESA/CSA James Webb Space Telescope’s <a href=\"https://esawebb.org/about/instruments/nircam-niriss/\">Near-Infrared Camera</a>. The image demonstrates Webb’s remarkable ability to resolve faint stars outside the Milky Way.&nbsp;This observation was taken as part of Webb’s Early Release Science (ERS) program&nbsp;<a href=\"https://www.stsci.edu/jwst/science-execution/program-information.html?id=1334\">1334</a>, focused on resolved stellar populations. The dwarf galaxy WLM was selected for this program as its gas is similar to that which made up galaxies in the early Universe and it is relatively nearby, meaning that Webb can differentiate between its individual stars.&nbsp;Learn more about Webb’s research of the dwarf galaxy WLM <a href=\"https://blogs.nasa.gov/webb/2022/11/09/beneath-the-night-sky-in-a-galaxy-not-too-far-away\">here</a>.The galaxy lies roughly 3 million light-years away.This image includes 0.9-micron light shown in blue, 1.5-micron in cyan, 2.5-micron in yellow, and 4.3-micron in red (filters F090W, F150W, F250M, and F430M).&nbsp;<em>Note: This image highlights Webb’s science in progress, which has not yet been through the peer-review process.</em><em>[Image Description: This image shows a wide field view of countless stars and dozens of galaxies in clear detail.]</em>",
                media.getDescription());
        assertEquals(Set.of("Galaxies", "NIRCam"), media.getCategories());
        assertEquals("NASA, ESA, CSA, STScI, and K. McQuinn (Rutgers University), A. Pagan (STScI).",
                media.getCredit());
        assertEquals("2022-11-09T17:00", media.getDate().toString());
        assertEquals(4134, media.getMetadata().get(0).getImageDimensions().getHeight());
        assertEquals(4134, media.getMetadata().get(0).getImageDimensions().getWidth());
        assertEquals("Cetus", media.getConstellation());
        assertEquals("[PGU2007] cep35", media.getName());
        assertEquals(DjangoplicityMediaType.Observation, media.getImageType());
        assertEquals(
                List.of("https://esawebb.org/media/archives/images/original/WLMb.tif",
                        "https://cdn.esawebb.org/archives/images/large/WLMb.jpg"),
                media.getMetadata().stream().map(m -> m.getAssetUrl().toExternalForm()).toList());
        assertEquals("https://cdn.esawebb.org/archives/images/screen/WLMb.jpg",
                media.getThumbnailUrl().toExternalForm());
        assertEquals(Set.of("James Webb Space Telescope"), media.getTelescopes());
        assertEquals(Set.of("NIRCam"), media.getInstruments());
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public WebbEsaService service(DjangoplicityMediaRepository repository,
                @Value("${webb.esa.search.link}") String searchLink) {
            return new WebbEsaService(repository, searchLink);
        }
    }
}
