package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

@SpringJUnitConfig(SearchServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class SearchServiceTest {

    @MockBean
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @MockBean(name = "domain")
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private SearchService service;

    @Test
    void testIsSearchEnabled() {
        assertTrue(service.isSearchEnabled());
    }

    @Configuration
    @Import(SpacemediaCommonConfiguration.class)
    public static class TestConfig {

        @Bean
        public SearchService service() {
            return new SearchService();
        }

        @Bean
        public RestTemplateBuilder rest() {
            return new RestTemplateBuilder();
        }
    }
}
