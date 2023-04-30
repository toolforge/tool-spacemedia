package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GeometryServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class GeometryServiceTest {

    @Autowired
    private GeometryService service;

    @Test
    void testGetContinent() {
        assertEquals("Europe", service.getContinent(45, 0));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public GeometryService service() {
            return new GeometryService();
        }
    }
}
