package org.wikimedia.commons.donvip.spacemedia.service.box;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Disabled("Slow and requires credentials")
@SpringJUnitConfig(BoxServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class BoxServiceTest {

    @Autowired
    private BoxService service;

    @MockBean
    private CommonsService commons;

    @Test
    void testGetFiles() {
        assertTrue(service.getFiles("nasa-external-ocomm", "onrtmdvofqluv5ei5kfu5u1pf8v4xqtl").size() > 1000);
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule());
        }

        @Bean
        public BoxService boxService(
                @Value("${box.api.oauth2.client-id}") String clientId,
                @Value("${box.api.oauth2.client-secret}") String clientSecret,
                @Value("${box.api.user-email}") String userEmail,
                @Value("${box.api.user-password}") String userPassword)
                throws IOException {
            return new BoxService(clientId, clientSecret, userEmail, userPassword);
        }
    }
}
