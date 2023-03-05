package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.kari.KariMediaRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringJUnitConfig(KariServiceTest.TestConfig.class)
class KariServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private KariMediaRepository repository;

    @Autowired
    private KariService service;

    @Test
    void testGetName() {
        assertEquals("KARI", service.getName());
    }

    @Configuration
    static class TestConfig {

        @Bean
        @Autowired
        public KariService service(KariMediaRepository repository) {
            return new KariService(repository);
        }

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }
    }
}
