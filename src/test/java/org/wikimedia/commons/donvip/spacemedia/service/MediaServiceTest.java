package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;
 
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.hashes.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.IndividualsFlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.NasaFlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.ContentsAndMetadata;
import org.wikimedia.commons.donvip.spacemedia.utils.MediaUtils;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@SpringJUnitConfig(MediaServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class MediaServiceTest {

    @MockBean
    private CommonsService commonsService;

    @MockBean
    private YouTubeMediaRepository youtubeRepository;

    @MockBean
    private HashAssociationRepository hashRepository;

    @MockBean
    private FileMetadataRepository metadataRepository;

    @MockBean
    private ExifMetadataRepository exifRepository;

    @Autowired
    private MediaService service;

    @Test
    void testBelongsToBlocklist() {
        Media media = new Media();
        media.addMetadata(new FileMetadata());
        media.setTitle("Spaceport of the Future Presentation");
        media.setDescription(
                "Tom Stevens, Space Launch Delta 30 executive director, provides a Lunch\r\nand Learn topic presentation entitled Spaceport of the Future Overview at Vandenberg Space Force Base, Calif., March 7, 2023. The presentation included historical background, space launch vehicles and launch forecasts, spaceport vision and strategy, future assets and infrastructure projects, graphics and mapping of key assets. (U.S. Space Force photo by Senior Airman Tiarra Sibley)");
        assertTrue(service.belongsToBlocklist(media));
        assertTrue(media.isIgnored());
        assertEquals(List.of("Title or description contains term(s) in block list: lunch and learn"),
                media.getIgnoredReasons());
    }

    @Test
    void testCleanupDescriptionPierreMarkuse() {
        FlickrMedia media = new FlickrMedia();
        media.setId(new CompositeMediaId("pierre_markuse", "52840868995"));
        media.setDescription(
                """
                        Contains modified Copernicus Sentinel data [2023], processed by <a href="https://twitter.com/Pierre_Markuse">Pierre Markuse</a>

                        Fires in Western Australia (Lat: -15.925, Lng:124.468) - 20 April 2023

                        Image is about 16 kilometers wide

                        Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href="https://www.paypal.com/paypalme/PierreMarkuse">PayPal me!</a>

                        Follow me on <a href="https://twitter.com/Pierre_Markuse">Twitter!</a> and <a href="https://mastodon.world/@pierre_markuse">Mastodon!</a>
                        """);

        MediaService.cleanupDescription(media, IndividualsFlickrService.PATTERNS_TO_REMOVE,
                IndividualsFlickrService.STRINGS_TO_REMOVE);

        assertEquals(
                """
                        Contains modified Copernicus Sentinel data [2023], processed by <a href="https://twitter.com/Pierre_Markuse">Pierre Markuse</a>

                        Fires in Western Australia (Lat: -15.925, Lng:124.468) - 20 April 2023

                        Image is about 16 kilometers wide
                        """
                        .trim(),
                media.getDescription());
    }

    @Test
    void testCleanupDescriptionNASA() {
        FlickrMedia media = new FlickrMedia();
        media.setId(new CompositeMediaId("nasawebbtelescope", "51329294265"));
        media.setDescription("""
                                This new image fresh from the Northrop Grumman cleanroom shows #NASAWebb nearly fully packed up into the same formation it will have for launch. Only a few tests remain before the team transitions into shipment operations.

                More on Webb’s recent progress can be found here: <a href="https://go.nasa.gov/3hX7l2q">go.nasa.gov/3hX7l2q</a>

                Credit: NASA/Chris Gunn

                <a href="https://go.nasa.gov/3kJku0S">NASA Media Use Policy</a>

                <a href="https://go.nasa.gov/3rqWK2W">Follow us on Twitter</a>

                <a href="https://go.nasa.gov/3rtRUSP%e2%80%9d rel=">Like us on Facebook</a>

                <a href="https://go.nasa.gov/3hX7rXQ">Subscribe to our YouTube channel</a>

                <a href="https://go.nasa.gov/3xY33O3">Follow us on Instagram</a>
                """);

        MediaService.cleanupDescription(media, NasaFlickrService.PATTERNS_TO_REMOVE,
                NasaFlickrService.STRINGS_TO_REMOVE);

        assertEquals(
                """
                        This new image fresh from the Northrop Grumman cleanroom shows #NASAWebb nearly fully packed up into the same formation it will have for launch. Only a few tests remain before the team transitions into shipment operations.

                        More on Webb’s recent progress can be found here: <a href="https://go.nasa.gov/3hX7l2q">go.nasa.gov/3hX7l2q</a>

                        Credit: NASA/Chris Gunn
                        """
                        .trim(),
                media.getDescription());
    }

    @ParameterizedTest
    @CsvSource({
        "15161474123_df8e27c096_o.jpg",
        "cbar_temperature.png",
        "STScI-01EVTAZ1A1PQ05ZT0GQNFGF57D.tif",
        "4879612358_a8f6ff743a_o.gif",
        "STScI-01EVVG5B33PKR01TBTYPF04TFE.pdf",
        "illustration_8.ppt",
        "pluto_and_charon_30611.pptx",
        "PIA11217_H264_E12_proper_nosat_BW_3fps.mp4",
        "PIA14310.wav",
        "PIA14942.mov",
        //"b_far.0018__cameraShape1_beauty.00.webm" // needs exiftool. To enable after switch to metadata-extractor
    })
    void testUpdateReadableStateAndDims(String file) throws Exception {
        URL url = newURL("file://"+file);
        FileMetadata fm = new FileMetadata(url);
        Path path = Paths.get("src/test/resources/samples/", file);
        try (InputStream in = Files.newInputStream(path)) {
            ContentsAndMetadata<?> img = MediaUtils.readFile(urlToUriUnchecked(url), Utils.findExtension(file),
                path, false, in, null, () -> 0);
            assertNotNull(img);

            assertTrue(MediaService.updateReadableStateAndDims(fm, img));

            if (!fm.isAudio()) {
                assertTrue(fm.hasValidDimensions());
            }
            if (fm.isAudio() || fm.isVideo()) {
                assertTrue(fm.hasValidDuration());
            }
        }
    }

    @Configuration
    @Import(SpacemediaCommonConfiguration.class)
    public static class TestConfig {

        @Bean
        public MediaService service() {
            return new MediaService();
        }

        @Bean
        public RestTemplateBuilder rest() {
            return new RestTemplateBuilder();
        }
    }
}
