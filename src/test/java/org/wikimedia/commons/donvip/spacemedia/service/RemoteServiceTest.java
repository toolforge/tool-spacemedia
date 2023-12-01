package org.wikimedia.commons.donvip.spacemedia.service;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@SpringJUnitConfig(RemoteServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
@WireMockTest(httpPort = 9090)
class RemoteServiceTest {

    @Autowired
    private RemoteService service;

    @Test
    void testEvictCaches(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get("/foo/rest/evictcaches").willReturn(ok()));
        assertDoesNotThrow(() -> service.evictCaches("foo"));
    }

    @Configuration
    @Import(SpacemediaCommonConfiguration.class)
    public static class TestConfig {

        @Bean
        public RemoteService service() {
            return new RemoteService();
        }

        @Bean
        public RestTemplateBuilder rest() {
            return new RestTemplateBuilder();
        }
    }
}
