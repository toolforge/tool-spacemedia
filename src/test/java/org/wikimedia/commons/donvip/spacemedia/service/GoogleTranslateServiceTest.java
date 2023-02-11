package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;

@SpringJUnitConfig(GoogleTranslateServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class GoogleTranslateServiceTest {

    @Autowired
    private GoogleTranslateService service;

    @Test
    @Disabled("requires credentials")
    void testTranslate() throws IOException {
        assertEquals("Development of Korea's first space launch vehicle Naro (KSLV-I)",
                service.translate("한국 최초 우주발사체 나로호(KSLV-I) 개발", "ko", "en"));
    }

    @Configuration
    @Import(SpacemediaCommonConfiguration.class)
    public static class TestConfig {

        @Bean
        public GoogleTranslateService service() {
            return new GoogleTranslateService();
        }

        @Bean
        public RestTemplateBuilder rest() {
            return new RestTemplateBuilder();
        }
    }
}
