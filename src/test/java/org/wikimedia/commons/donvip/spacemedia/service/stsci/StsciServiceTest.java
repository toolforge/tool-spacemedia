package org.wikimedia.commons.donvip.spacemedia.service.stsci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgServiceTest.html;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciImageFiles;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringJUnitConfig(StsciServiceTest.TestConfig.class)
class StsciServiceTest {

    @MockBean
    private StsciMediaRepository repository;

    @Autowired
    private StsciService service;

    @Test
    void testFileDownloadTextPattern() throws IOException {
        doTest("7400 X 4162, PNG (20.44 MB)", 7400, 4162, 21432893);
        doTest("4684 X 2807, TIF (21.41 MB)", 4684, 2807, 22450012);
        doTest("Text Description, PDF (90.65 KB)", 0, 0, 92825);
        doTest("Full Res, 3840 X 2160, TIF (8.30 MB)", 3840, 2160, 8703180);
        doTest("Annotated, 1666 X 1332, TIF (12.74 MB)", 1666, 1332, 13358858);
        doTest("5217 X 2499, TIFF (20.53 MB)", 5217, 2499, 21527265);
        doTest("Annotated Medium, 1604 X 2000, PNG (5.58 MB)", 1604, 2000, 5851054);
        doTest("PDF (85.62 KB)", 0, 0, 87674);
        doTest("NIRCam Only, Full Res, 12654 X 12132, TIF (160.01 MB)", 12654, 12132, 167782645);
        doTest("Text Description of Webb Diffraction Spikes Infographic, PDF (37.56 KB)", 0, 0, 38461);
        doTest("Full Res., 1920 X 1080, PNG (450.31 KB)", 1920, 1080, 461117);
        doTest("Vertical Version, Full Res, 6900 X 10809, PNG (2.35 MB)", 6900, 10809, 2464153);
        doTest("Full identifier, transparent background, 555 X 309, PNG (28.96 KB)", 555, 309, 29655);
        doTest("Full Res (Annotated), 4000 X 2000, PNG (2.00 MB)", 4000, 2000, 2097152);
        doTest("Text-editable PDF, PDF (604.28 KB)", 0, 0, 618782);
        doTest("Half Res [FOR DOWNLOAD ONLY], 16037 X 20574, PNG (542.52 MB)", 16037, 20574, 568873451);
        doTest("MACS J1341, 1750 X 1750, TIF (7.59 MB)", 1750, 1750, 7958691);
        doTest("2000x1125, 2000 X 1125, PNG (993.39 KB)", 2000, 1125, 1017231);
    }

    private static void doTest(String text, int width, int height, int sizeInBytes) throws IOException {
        StsciImageFiles file = StsciService.extractFile(null, null, text);
        assertNotNull(file);
        assertEquals(width, file.getWidth());
        assertEquals(height, file.getHeight());
        assertEquals(sizeInBytes, file.getFileSize());
    }

    @Test
    void testReadHtmlHubble() throws Exception {
        String urlLink = "https://hubblesite.org/contents/media/images/2022/054/01GGT9JB74B5FXX95WGM0285E5";
        StsciMedia media = service.getImageDetailsByScrapping("2022/054/01GGT9JB74B5FXX95WGM0285E5", urlLink,
                new URL(urlLink), html("nasahubble/2022_054_01GGT9JB74B5FXX95WGM0285E5.html"));
        assertNotNull(media);
        assertEquals("2022/054/01GGT9JB74B5FXX95WGM0285E5", media.getId().getMediaId());
        assertEquals("Lensed Supernova in Abell 370", media.getTitle());
        assertEquals(
                "Through a phenomenon called gravitational lensing, three different moments in a far-off supernova explosion were captured in a single snapshot by NASA's Hubble Space Telescope. The light from the supernova, which was located behind the galaxy cluster Abell 370, was multiply lensed by the cluster's immense gravity. This light took three different paths through the cosmic lens of the massive cluster. The three paths were three different lengths and affected to different degrees by the slowing of time and curvature of space due to the cluster, so when the light arrived at Hubble (on the same day in December 2010), the supernova appeared at three different stages of evolution.\n"
                        + "The left panel shows the portion of Abell 370 where the multiple images of the supernova appeared. Panel A, a composite of Hubble observations from 2011 to 2016, shows the locations of the multiply imaged host galaxy after the supernova faded. Panel B, a Hubble picture from December 2010, shows the three images of the host galaxy and the supernova at different phases in its evolution. Panel C, which subtracts the image in Panel B from that in Panel A, shows three different faces of the evolving supernova. Using a similar image subtraction process for multiple filters of data, Panel D shows the different colors of the cooling supernova at three different stages in its evolution.",
                media.getDescription());
        assertEquals("2022-11-09T11:00-05:00[America/New_York]", media.getPublicationDateTime().toString());
        assertEquals(
                List.of("https://stsci-opo.org/STScI-01GGT9RV3H6JVMQWEM3WKJGZVW.tif",
                        "https://stsci-opo.org/STScI-01GGT9R6NXMYV39JGQF1TCH3V8.png"),
                media.getMetadata().stream().map(m -> m.getAssetUrl().toExternalForm()).toList());
        assertEquals("https://stsci-opo.org/STScI-01GGT9TZJC2031623AM72RSVY8.png",
                media.getThumbnailUrl().toExternalForm());
        assertEquals("2022-054", media.getNewsId());
        assertNull(media.getCreationDate());
        assertEquals("Abell 370", media.getObjectName());
        assertEquals("hubble", media.getMission());
        assertEquals(Set.of("Gravitational Lensing", "Supernovae", "Galaxy Clusters"), media.getKeywords());
        assertEquals("SCIENCE: NASA, ESA, STScI, Wenlei Chen (UMN), Patrick Kelly (UMN), Hubble Frontier Fields",
                media.getCredits());
    }

    @Test
    void testReadHtmlWebb() throws Exception {
        String urlLink = "https://webbtelescope.org/contents/media/images/2022/060/01GJ3HZRT43P8JATFQ90Z8EZ0W";
        StsciMedia media = service.getImageDetailsByScrapping("2022/060/01GJ3HZRT43P8JATFQ90Z8EZ0W", urlLink,
                new URL(urlLink), html("nasawebb/2022_060_01GJ3HZRT43P8JATFQ90Z8EZ0W.html"));
        assertNotNull(media);
        assertEquals("2022/060/01GJ3HZRT43P8JATFQ90Z8EZ0W", media.getId().getMediaId());
        assertEquals("Exoplanet WASP-39 b and its Star (Illustration)", media.getTitle());
        assertEquals(
                "This illustration shows what exoplanet WASP-39 b could look like, based on current understanding of the planet.\n"
                        + "WASP-39 b is a hot, puffy gas giant with a mass 0.28 times Jupiter (0.94 times Saturn) and a diameter 1.3 times greater than Jupiter, orbiting just 0.0486 astronomical units (4,500,000 miles) from its star. The star, WASP-39, is fractionally smaller and less massive than the Sun. Because it is so close to its star, WASP-39 b is very hot and is likely to be tidally locked, with one side facing the star at all times.\n"
                        + "NASA’s James Webb Space Telescope’s exquisitely sensitive instruments have provided a profile of WASP-39 b’s atmospheric constituents and identified a plethora of contents, including water, sulfur dioxide, carbon monoxide, sodium and potassium.\n"
                        + "This illustration is based on indirect transit observations from Webb as well as other space- and ground-based telescopes. Webb has not captured a direct image of this planet.",
                media.getDescription());
        assertEquals("2022-11-22T11:00-05:00[America/New_York]", media.getPublicationDateTime().toString());
        assertEquals("https://stsci-opo.org/STScI-01GJ3Q3PRF2VG9DNG7J5YX1N44.jpg",
                media.getMetadata().iterator().next().getAssetUrl().toExternalForm());
        assertEquals("https://stsci-opo.org/STScI-01GJ3Q46VFXMCFM4WZANYCC4MR.jpg",
                media.getThumbnailUrl().toExternalForm());
        assertEquals("2022-060", media.getNewsId());
        assertNull(media.getCreationDate());
        assertEquals("WASP-39 b", media.getObjectName());
        assertEquals("webb", media.getMission());
        assertEquals(Set.of("Exoplanets"), media.getKeywords());
        assertEquals("ILLUSTRATION: NASA, ESA, CSA, Joseph Olmsted (STScI)", media.getCredits());
    }

    @Test
    void testReadHtmlWebbFullResJpg() throws Exception {
        String urlLink = "https://webbtelescope.org/contents/media/images/2018/14/4119-Image";
        StsciMedia media = service.getImageDetailsByScrapping("2018/14/4119-Image", urlLink, new URL(urlLink),
                html("nasawebb/2018_14_4119-Image.html"));
        assertNotNull(media);
        assertEquals("2018/14/4119-Image", media.getId().getMediaId());
        assertEquals("Reflection Nebula IC 2631 (ESO)", media.getTitle());
        assertEquals(
                "An image from the European Southern Observatory shows blue light from a newborn star lights up the reflection nebula IC 2631. This nebula is part of the Chamaeleon star-forming region, which Webb will study to learn more about the formation of water and other cosmic ices.",
                media.getDescription());
        assertEquals("2018-03-09T10:00-05:00[America/New_York]", media.getPublicationDateTime().toString());
        assertEquals("https://stsci-opo.org/STScI-01EVT0Y2KZAV2YF3ARK6X2PT7S.jpg",
                media.getMetadata().iterator().next().getAssetUrl().toExternalForm());
        assertEquals("https://stsci-opo.org/STScI-01EVT0Y2KZAV2YF3ARK6X2PT7S.jpg",
                media.getThumbnailUrl().toExternalForm());
        assertEquals("2018-14", media.getNewsId());
        assertNull(media.getCreationDate());
        assertEquals("IC 2631", media.getObjectName());
        assertEquals("webb", media.getMission());
        assertEquals(Set.of("Nebulas", "Reflection Nebulas", "Star Forming Regions"), media.getKeywords());
        assertEquals("IMAGE: ESO", media.getCredits());
    }

    @Test
    void testReadHtmlWebbNewCreditsFormat() throws Exception {
        String urlLink = "https://webbtelescope.org/contents/media/images/2023/112/01GYJ7H5VSDMPRWX0R0Z6R87EC";
        StsciMedia media = service.getImageDetailsByScrapping("2023/112/01GYJ7H5VSDMPRWX0R0Z6R87EC", urlLink,
                new URL(urlLink), html("nasawebb/2023_112_01GYJ7H5VSDMPRWX0R0Z6R87EC.html"));
        assertNotNull(media);
        assertEquals("2023/112/01GYJ7H5VSDMPRWX0R0Z6R87EC", media.getId().getMediaId());
        assertEquals("Enceladus Plume (Webb [NIRSpec] and Cassini Image)", media.getTitle());
        assertEquals(
                "An image from NASA’s James Webb Space Telescope’s NIRSpec (Near-Infrared Spectrograph) shows a water vapor plume jetting from the southern pole of Saturn’s moon Enceladus, extending out more than 20 times the size of the moon itself. The inset, an image from the Cassini orbiter, emphasizes how small Enceladus appears in the Webb image compared to the water plume.\n"
                        + "Webb is allowing researchers, for the first time, to directly see how this plume feeds the water supply for the entire system of Saturn and its rings. By analyzing the Webb data, astronomers have determined roughly 30 percent of the water stays within this torus, a fuzzy donut of water that is co-located with Saturn’s “E-ring,” and the other 70 percent escapes to supply the rest of the Saturnian system with water.\n"
                        + "Enceladus, an ocean world about four percent the size of Earth, just 313 miles across, is one of the most exciting scientific targets in our solar system in the search for life beyond Earth. A global reservoir of salty water sits below the moon’s icy outer crust, and geyser-like volcanoes spew jets of ice particles, water vapor, and organic chemicals out of crevices in the moon’s surface informally called ‘tiger stripes.’\n"
                        + "NIRSpec was built for the European Space Agency (ESA) by a consortium of European companies led by Airbus Defence and Space (ADS) with NASA’s Goddard Space Flight Centre providing its detector and micro-shutter subsystems.",
                media.getDescription());
        assertEquals("2023-05-30T11:00-04:00[America/New_York]", media.getPublicationDateTime().toString());
        assertEquals("https://stsci-opo.org/STScI-01GYJ8KNFKX79X3WFF880Z7XQF.tif",
                media.getMetadata().iterator().next().getAssetUrl().toExternalForm());
        assertEquals("https://stsci-opo.org/STScI-01GYJ8KXBB9GYG58KF9V7WRSGT.png",
                media.getThumbnailUrl().toExternalForm());
        assertEquals("2023-112", media.getNewsId());
        assertNull(media.getCreationDate());
        assertEquals("Enceladus", media.getObjectName());
        assertEquals("webb", media.getMission());
        assertEquals(Set.of("Moons", "Saturn", "Solar System"), media.getKeywords());
        assertEquals("Image: NASA, ESA, CSA, Geronimo Villanueva (NASA-GSFC) Image Processing: Alyssa Pagan (STScI)",
                media.getCredits());
    }

    @Test
    void testCreditsWithHtmlLink() throws Exception {
        String urlLink = "https://hubblesite.org/contents/media/images/2020/07/4622-Image";
        StsciMedia media = service.getImageDetailsByScrapping("2020/07/4622-Image", urlLink, new URL(urlLink),
                html("nasahubble/2020_07_4622-Image.htm"));
        assertEquals("2020/07/4622-Image", media.getId().getMediaId());
        assertEquals("Illustration of Dwarf Nova System", media.getTitle());
        assertEquals(
                "This illustration shows a newly discovered dwarf nova system, in which a white dwarf star is pulling material off a brown dwarf companion. The material collects into an accretion disk until reaching a tipping point, causing it to suddenly increase in brightness. Using archival Kepler data, a team observed a previously unseen, and unexplained, gradual intensification followed by a super-outburst in which the system brightened by a factor of 1,600 over less than a day.",
                media.getDescription());
        assertEquals("2020-01-24T10:00-05:00[America/New_York]", media.getPublicationDateTime().toString());
        assertEquals("https://stsci-opo.org/STScI-01EVSTVJQDGP1WERZF77WZ1D63.tif",
                media.getMetadata().iterator().next().getAssetUrl().toExternalForm());
        assertEquals("https://stsci-opo.org/STScI-01EVSTVMRN7WT62EYZ99T58FCK.png",
                media.getThumbnailUrl().toExternalForm());
        assertEquals("2020-07", media.getNewsId());
        assertNull(media.getCreationDate());
        assertNull(media.getObjectName());
        assertEquals("hubble", media.getMission());
        assertEquals(Set.of("Brown Dwarfs", "White Dwarfs", "Novae", "Stars"), media.getKeywords());
        assertEquals("NASA and L. Hustak (STScI)", media.getCredits());
    }

    @Test
    void testNPE() throws Exception {
        String urlLink = "https://hubblesite.org/contents/media/images/4652-Image";
        StsciMedia media = service.getImageDetailsByScrapping("4652-Image", urlLink, new URL(urlLink),
                html("nasahubble/4652-Image.htm"));
        assertNotNull(media);
    }

    @Configuration
    static class TestConfig {

        @Bean
        @Autowired
        public StsciService service() {
            return new StsciService();
        }

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }
    }
}
