package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
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

    private static class TestMedia extends Media<String, LocalDate> {

        @Override
        public String getId() {
            return null;
        }

        @Override
        public void setId(String id) {
        }

        @Override
        public LocalDate getDate() {
            return null;
        }

        @Override
        public void setDate(LocalDate date) {
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
