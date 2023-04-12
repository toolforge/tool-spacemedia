package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;

@SpringJUnitConfig(IndividualsFlickrServiceTest.TestConfig.class)
class IndividualsFlickrServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private FlickrMediaRepository repository;

    @MockBean
    private FlickrMediaProcessorService processor;

    @MockBean
    private FlickrService flickr;

    @Autowired
    private IndividualsFlickrService service;

    @Test
    void testResolveShortenerFlickr() {
        FlickrMedia media = new FlickrMedia();
        media.setDescription(
                """
                        Finally decided to finish up a version of my own for this beautiful planetary nebula. I did a LOT of cleaning up of the banding issues once again using the G'MIC noise debanding filter. I also decided to do something a little different with that central pool of soft but bright, hazy light. I used one of the filters to mostly remove it, revealing the central details more clearly. This leaves the overall image pretty heavy on the red and orange side of things, but I think it looks pretty cool like this.

                        This was, of course, part of the early release data after JWST was first launched. You can see the official version and learn a lot more about this planetary nebula over yonder: <a href="https://webbtelescope.org/contents/media/images/2022/033/01G70BGTSYBHS69T7K3N3ASSEB">webbtelescope.org/contents/media/images/2022/033/01G70BGT...</a>

                        Once upon a time, Hubble looked upon this nebula, as well... and I did a version of that, too. <a href="https://flic.kr/p/gJ3NzH">flic.kr/p/gJ3NzH</a>

                        Central subtraction: JWST/NIRCam F405N-F444W
                        Red-Orange: JWST/NIRCam F444W-470N, F212N
                        Yellow: JWST/NIRCam F356W
                        Teal: JWST/NIRCam F187N
                        Blue: JWST/NIRCam F090W

                        North is 111.51° clockwise from up.
                                                """);
        assertEquals(
                """
                        Finally decided to finish up a version of my own for this beautiful planetary nebula. I did a LOT of cleaning up of the banding issues once again using the G'MIC noise debanding filter. I also decided to do something a little different with that central pool of soft but bright, hazy light. I used one of the filters to mostly remove it, revealing the central details more clearly. This leaves the overall image pretty heavy on the red and orange side of things, but I think it looks pretty cool like this.

                        This was, of course, part of the early release data after JWST was first launched. You can see the official version and learn a lot more about this planetary nebula over yonder: <a href="https://webbtelescope.org/contents/media/images/2022/033/01G70BGTSYBHS69T7K3N3ASSEB">webbtelescope.org/contents/media/images/2022/033/01G70BGT...</a>

                        Once upon a time, Hubble looked upon this nebula, as well... and I did a version of that, too. <a href="https://www.flickr.com/photo.gne?short=gJ3NzH">https://www.flickr.com/photo.gne?short=gJ3NzH</a>

                        Central subtraction: JWST/NIRCam F405N-F444W
                        Red-Orange: JWST/NIRCam F444W-470N, F212N
                        Yellow: JWST/NIRCam F356W
                        Teal: JWST/NIRCam F187N
                        Blue: JWST/NIRCam F090W

                        North is 111.51° clockwise from up.
                        """,
                service.getDescription(media));
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public IndividualsFlickrService service(FlickrMediaRepository repository,
                @Value("${individuals.flickr.accounts}") Set<String> flickrAccounts) {
            return new IndividualsFlickrService(repository, flickrAccounts);
        }
    }
}
