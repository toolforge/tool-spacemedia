package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;

@SpringJUnitConfig(NasaFlickrServiceTest.TestConfig.class)
class NasaFlickrServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private FlickrMediaRepository repository;

    @MockBean
    private NasaMediaRepository<?> nasaRepository;

    @MockBean
    private FlickrMediaProcessorService processor;

    @MockBean
    private FlickrService flickr;

    @Autowired
    private NasaFlickrService service;

    @Test
    void testGetNasaIdAndSource() throws MalformedURLException {
        FlickrMedia media = new FlickrMedia();
        media.setId(52867079337L);
        media.setPathAlias("nasahqphoto");
        media.setTitle("Czech Republic Artemis Accords Signing (NHQ202305030019)");
        media.addMetadata(
                new FileMetadata("https://live.staticflickr.com/65535/52867079337_932f992c50_o_d.jpg"));

        assertEquals(
                "[https://www.flickr.com/photos/nasahqphoto/52867079337 Czech Republic Artemis Accords Signing (NHQ202305030019)]\n"
                        + "{{NASA-image|id=NHQ202305030019|center=HQ}}",
                service.getSource(media, media.getUniqueMetadata()));
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaFlickrService service(FlickrMediaRepository repository,
                @Value("${nasa.flickr.accounts}") Set<String> flickrAccounts) {
            return new NasaFlickrService(repository, flickrAccounts);
        }
    }
}
