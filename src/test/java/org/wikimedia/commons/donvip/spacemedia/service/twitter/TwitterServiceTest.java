package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaImage;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        assertNotNull(twitter.tweet(List.of(), List.of(), Set.of()));
    }

    @Test
    void testTweetContents() throws JsonProcessingException {
        TweetRequest request = jackson.readValue(
                twitter.buildTweetRequest(List.of(new NasaImage()), List.of(), Set.of()).getStringPayload(),
                TweetRequest.class);
        assertEquals("1 new picture", request.getText());

        request = jackson.readValue(
                twitter.buildTweetRequest(List.of(new NasaImage(), new NasaImage()), List.of(), Set.of("ESA", "NASA"))
                        .getStringPayload(),
                TweetRequest.class);
        assertEquals("2 new pictures from @ESA @NASA", request.getText());
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
