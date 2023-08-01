package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URL;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaType;

@SpringJUnitConfig(HubbleEsaServiceTest.TestConfig.class)
class HubbleEsaServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private DjangoplicityMediaRepository repository;

    @Autowired
    private HubbleEsaService service;

    @Test
    void testReadHtml() throws Exception {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        DjangoplicityMedia media = service.newMediaFromHtml(html("esahubble/potw2319a.html"),
                new URL("https://esahubble.org/images/potw2319a/"), "potw2319a", null);
        assertNotNull(media);
        assertEquals("potw2319a", media.getId().getId());
        assertEquals("Cosmic leviathan", media.getTitle());
        assertEquals(
                "A vast <a href=\"https://esahubble.org/wordbank/galaxy/\">galaxy</a> cluster lurks in the centre of this image from the NASA/ESA Hubble Space Telescope. Like a submerged sea monster causing waves on the surface, this cosmic leviathan can be identified by the distortions in spacetime around it. The mass of the cluster has caused the images of background galaxies to be <a href=\"https://esahubble.org/wordbank/gravitational-lensing/\">gravitationally lensed</a>; the galaxy cluster has caused a sufficient curvature of spacetime to bend the path of light and cause background galaxies to appear distorted into streaks and arcs of light. A host of other galaxies can be seen surrounding the cluster, and a handful of foreground stars with tell-tale diffraction spikes are scattered throughout the image.This particular galaxy cluster is called eMACS J1823.1+7822, and lies almost nine billion light-years away in the constellation Draco. It is one of five exceptionally massive galaxy clusters explored by Hubble in the hopes of measuring the strengths of these gravitational lenses and providing insights into the distribution of dark matter in galaxy clusters. Strong gravitational lenses like eMACS J1823.1+7822 can help astronomers study distant galaxies by acting as vast natural telescopes which magnify objects that would otherwise be too faint or distant to resolve.This multiwavelength image layers data from eight different filters and two different instruments: Hubble’s <a href=\"https://esahubble.org/about/general/instruments/acs/\">Advanced Camera for Surveys</a> and <a href=\"https://esahubble.org/about/general/instruments/wfc3/\">Wide Field Camera 3</a>. Both instruments have the ability to view astronomical objects in just a small slice of the <a href=\"https://esahubble.org/wordbank/electromagnetic-spectrum/\">electromagnetic spectrum</a> using filters, which allow astronomers to image objects at precisely selected wavelengths. The combination of observations at different wavelengths lets astronomers develop a more complete picture of the structure, composition and behaviour of an object than visible light alone would reveal.[<em>Image description:</em>&nbsp;A cluster of large galaxies, surrounded by various stars and smaller galaxies on a dark background. The central cluster is mostly made of bright elliptical galaxies that are surrounded by a warm glow. Close to the cluster core is the stretched, distorted arc of a galaxy, gravitationally lensed by the cluster.]",
                media.getDescription());
        assertEquals(Set.of("Galaxies"), media.getCategories());
        assertEquals("ESA/Hubble & NASA, H. Ebeling", media.getCredit());
        assertEquals("2023-05-08T06:00", media.getDate().toString());
        ImageDimensions dims = media.getMetadata().iterator().next().getImageDimensions();
        assertEquals(2389, dims.getHeight());
        assertEquals(2839, dims.getWidth());
        assertEquals("Draco", media.getConstellation());
        assertNull(media.getName());
        assertEquals(DjangoplicityMediaType.Observation, media.getImageType());
        assertEquals(
                "[https://esahubble.org/media/archives/images/original/potw2319a.tif, https://cdn.spacetelescope.org/archives/images/large/potw2319a.jpg]",
                media.getMetadata().stream().map(FileMetadata::getAssetUrl).toList().toString());
        assertEquals("https://cdn.spacetelescope.org/archives/images/screen/potw2319a.jpg",
                media.getThumbnailUrl().toExternalForm());
        assertEquals(Set.of("Hubble Space Telescope"), media.getTelescopes());
        assertEquals(Set.of("ACS", "WFC3"), media.getInstruments());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public HubbleEsaService service(DjangoplicityMediaRepository repository,
                @Value("${hubble.esa.search.link}") String searchLink) {
            return new HubbleEsaService(repository, searchLink);
        }
    }
}
