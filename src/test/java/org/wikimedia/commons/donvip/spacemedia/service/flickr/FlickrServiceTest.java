package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.flickr4java.flickr.FlickrException;

@SpringJUnitConfig(FlickrServiceTest.TestConfig.class)
class FlickrServiceTest {

    @Autowired
    private FlickrService service;

    @Test
    void test() throws FlickrException {
        assertNotNull(service.findPhoto("13149638943"));
    }

    @Configuration
    static class TestConfig {

        @Bean
        public FlickrService service(@Value("${flickr.api.key}") String flickrApiKey,
                @Value("${flickr.secret}") String flickrSecret) {
            return new FlickrService(flickrApiKey, flickrSecret);
        }
    }
}
