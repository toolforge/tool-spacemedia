package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaImage;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringJUnitConfig(TwitterServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class TwitterServiceTest {

    @Autowired
    private TwitterService twitter;

    @Autowired
    private ObjectMapper jackson;

    @MockBean
    private CommonsImageRepository repo;

    @Test
    @Disabled("This test will post a real tweet")
    void testTweet() throws Exception {
        assertDoesNotThrow(() -> twitter.postStatus(List.of(), List.of(), Set.of(), Set.of()));
    }

    @Test
    void testTweetContents() throws IOException {
        when(repo.findMaxTimestampBySha1In(any())).thenReturn("20230407000353");

        TweetRequest request = jackson.readValue(
                twitter.buildStatusRequest(List.of(new NasaImage()), List.of(newMetadata()), Set.of(), Set.of())
                        .getStringPayload(),
                TweetRequest.class);
        assertEquals("1 new picture\n\nList → https://commons.wikimedia.org/wiki/Special:ListFiles?limit=1&user=OptimusPrimeBot&ilshowall=1&offset=20230407000354", request.getText());

        request = jackson.readValue(
                twitter.buildStatusRequest(List.of(new NasaImage(), new NasaImage()),
                        List.of(newMetadata(), newMetadata()), Set.of(), Set.of("@ESA", "@NASA"))
                        .getStringPayload(),
                TweetRequest.class);
        assertEquals("2 new pictures from @ESA @NASA\n\nList → https://commons.wikimedia.org/wiki/Special:ListFiles?limit=2&user=OptimusPrimeBot&ilshowall=1&offset=20230407000354", request.getText());
    }

    private static final Metadata newMetadata() {
        Metadata m = new Metadata();
        m.setSha1("1");
        return m;
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }

        @Bean
        public TwitterService twitterService(
                @Value("${twitter.api.oauth1.consumer-token}") String consumerToken,
                @Value("${twitter.api.oauth1.consumer-secret}") String consumerSecret,
                @Value("${twitter.api.oauth1.access-token}") String accessToken,
                @Value("${twitter.api.oauth1.access-secret}") String accessSecret) {
            return new TwitterService(consumerToken, consumerSecret, accessToken, accessSecret);
        }
    }
}
