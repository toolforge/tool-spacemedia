package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringJUnitConfig(DlrFlickrServiceTest.TestConfig.class)
class DlrFlickrServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private FlickrMediaRepository repository;

    @MockBean
    private FlickrMediaProcessorService processor;

    @MockBean
    private FlickrService flickr;

    @Autowired
    private DlrFlickrService service;

    @Test
    void testResolveShortener() {
        FlickrMedia media = new FlickrMedia();
        media.setDescription(
                "Wichtige Informationen und Fotos von ATTAS: <a href=\"http://bit.ly/1d2k11a\" rel=\"noreferrer nofollow\">bit.ly/1d2k11a</a>\r\n"
                        + "    Bericht mit Fotos der Überführung von ATTAS: <a href=\"http://bit.ly/15Ocizr\" rel=\"noreferrer nofollow\">bit.ly/15Ocizr</a>\r\n"
                        + "    Quelle: DLR (CC-BY 3.0)");
        assertEquals(
                "Wichtige Informationen und Fotos von ATTAS: <a href=\"http://www.deutsches-museum.de/flugwerft/sammlungen/strahlflugzeuge/vfw-614-attas/\" rel=\"noreferrer nofollow\">http://www.deutsches-museum.de/flugwerft/sammlungen/strahlflugzeuge/vfw-614-attas/</a>\r\n"
                        + "    Bericht mit Fotos der Überführung von ATTAS: <a href=\"http://www.deutsches-museum.de/presse/presse-2013/attas/\" rel=\"noreferrer nofollow\">http://www.deutsches-museum.de/presse/presse-2013/attas/</a>\r\n"
                        + "    Quelle: DLR (CC-BY 3.0)",
                service.getDescription(media));
    }

    @Configuration
    static class TestConfig {

        @Bean
        @Autowired
        public DlrFlickrService service(FlickrMediaRepository repository,
                @Value("${dlr.flickr.accounts}") Set<String> flickrAccounts) {
            return new DlrFlickrService(repository, flickrAccounts);
        }

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }
    }
}
