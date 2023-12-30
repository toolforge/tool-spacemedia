package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringJUnitConfig(InternetArchiveServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class InternetArchiveServiceTest {

    @Autowired
    private InternetArchiveService service;

    @Test
    void testRetrieveOldestUrl() throws Exception {
        assertEquals(
                "http://web.archive.org/web/20080916024556/http://asterweb.jpl.nasa.gov/gallery/images/tokyo-snow.jpg",
                service.retrieveOldestUrl(new URL("http://asterweb.jpl.nasa.gov/gallery/images/tokyo-snow.jpg")).get()
                        .toExternalForm());
    }

    @Configuration
    @Import(SpacemediaCommonConfiguration.class)
    public static class TestConfig {

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule());
        }

        @Bean
        public InternetArchiveService service() {
            return new InternetArchiveService();
        }

        @Bean
        public RestTemplateBuilder rest() {
            return new RestTemplateBuilder();
        }
    }
}
