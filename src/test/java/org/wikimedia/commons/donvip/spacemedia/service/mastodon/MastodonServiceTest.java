package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URL;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringJUnitConfig(MastodonServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class MastodonServiceTest {

    @Autowired
    private MastodonService mastodon;

    @MockBean
    private CommonsImageRepository repo;

    @Test
    @Disabled("This test will post a real toot")
    void testToot() throws Exception {
        when(repo.findMaxTimestampBySha1In(any())).thenReturn("20230414235900");
        assertDoesNotThrow(() -> mastodon.postStatus(List.of(), List.of(), Set.of()));
    }

    @Test
    @Disabled("This test will post a real media. Not sure why it returns HTTP 422, Mastodon should accept this animated gif")
    void testPostMediaGif() {
        assertDoesNotThrow(() -> mastodon.postMedia(
                new URL("https://upload.wikimedia.org/wikipedia/commons/e/e0/Juice_gets_wings_ESA24834744.gif"),
                "image/gif", "Juice_gets_wings_ESA24834744.gif"));
    }

    @Test
    @Disabled("This test will post a real media")
    void testPostMediaJpeg() {
        assertDoesNotThrow(() -> mastodon.postMedia(
                new URL("https://upload.wikimedia.org/wikipedia/commons/2/2c/Juice_leaves_Earth_ESA24834654.jpg"),
                "image/jpeg", "Juice_leaves_Earth_ESA24834654.jpg"));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule());
        }

        @Bean
        public MastodonService mastodonService(@Value("${mastodon.instance}") String instance,
                @Value("${mastodon.api.oauth2.client-id}") String clientId,
                @Value("${mastodon.api.oauth2.client-secret}") String clientSecret,
                @Value("${mastodon.api.oauth2.access-token}") String accessToken) {
            return new MastodonService(instance, clientId, clientSecret, accessToken);
        }
    }
}
