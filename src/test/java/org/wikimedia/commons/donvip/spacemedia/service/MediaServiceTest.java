package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@SpringJUnitConfig(MediaServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class MediaServiceTest {

    @MockBean
    private CommonsService commonsService;

    @MockBean
    private YouTubeVideoRepository youtubeRepository;

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
        TestMedia media = new TestMedia();
        media.setTitle("Spaceport of the Future Presentation");
        media.setDescription(
                "Tom Stevens, Space Launch Delta 30 executive director, provides a Lunch\r\nand Learn topic presentation entitled Spaceport of the Future Overview at Vandenberg Space Force Base, Calif., March 7, 2023. The presentation included historical background, space launch vehicles and launch forecasts, spaceport vision and strategy, future assets and infrastructure projects, graphics and mapping of key assets. (U.S. Space Force photo by Senior Airman Tiarra Sibley)");
        assertTrue(service.belongsToBlocklist(media));
        assertTrue(media.isIgnored());
        assertEquals("Title or description contains term(s) in block list: lunch and learn", media.getIgnoredReason());
    }

    @Test
    void testCleanupDescription() {
        FlickrMedia media = new FlickrMedia();
        media.setId("52840868995");
        media.setPathAlias("pierre_markuse");
        media.setDescription(
                """
                        Contains modified Copernicus Sentinel data [2023], processed by <a href="https://twitter.com/Pierre_Markuse">Pierre Markuse</a>

                        Fires in Western Australia (Lat: -15.925, Lng:124.468) - 20 April 2023

                        Image is about 16 kilometers wide

                        Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href="https://www.paypal.com/paypalme/PierreMarkuse">PayPal me!</a>

                        Follow me on <a href="https://twitter.com/Pierre_Markuse">Twitter!</a> and <a href="https://mastodon.world/@pierre_markuse">Mastodon!</a>
                        """);
        List<String> stringsToRemove = List.of("Follow me on Twitter:",
                "<a href=\"https://twitter.com/Pierre_Markuse\" rel=\"nofollow\">twitter.com/Pierre_Markuse</a>",
                "Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href=\"https://www.paypal.com/paypalme/PierreMarkuse\">PayPal me!</a>",
                "Follow me on <a href=\"https://twitter.com/Pierre_Markuse\">Twitter!</a> and <a href=\"https://mastodon.world/@pierre_markuse\">Mastodon!</a>");

        MediaService.cleanupDescription(media, stringsToRemove);

        assertEquals(
                """
                        Contains modified Copernicus Sentinel data [2023], processed by <a href="https://twitter.com/Pierre_Markuse">Pierre Markuse</a>

                        Fires in Western Australia (Lat: -15.925, Lng:124.468) - 20 April 2023

                        Image is about 16 kilometers wide
                        """
                        .trim(),
                media.getDescription());
    }

    private static class TestMedia extends Media<String> {

        @Override
        public String getId() {
            return null;
        }

        @Override
        public void setId(String id) {
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
