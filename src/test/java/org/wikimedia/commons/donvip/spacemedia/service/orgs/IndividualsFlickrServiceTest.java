package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

@SpringJUnitConfig(IndividualsFlickrServiceTest.TestConfig.class)
class IndividualsFlickrServiceTest extends AbstractOrgServiceTest {

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
                service.getDescription(media, null));
    }

    @Test
    void testGetStringsToRemove() {
        FlickrMedia media = new FlickrMedia();
        media.setId(new CompositeMediaId("pierre_markuse", "52840868995"));
        media.setDescription(
                """
                        Contains modified Copernicus Sentinel data [2023], processed by <a href="https://twitter.com/Pierre_Markuse">Pierre Markuse</a>

                        Fires in Western Australia (Lat: -15.925, Lng:124.468) - 20 April 2023

                        Image is about 16 kilometers wide

                        Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href="https://www.paypal.com/paypalme/PierreMarkuse">PayPal me!</a>

                        Follow me on <a href="https://twitter.com/Pierre_Markuse">Twitter!</a> and <a href="https://mastodon.world/@pierre_markuse">Mastodon!</a>
                        """);
        Collection<String> stringsToRemove = service.getStringsToRemove(media);
        assertTrue(stringsToRemove.contains(
                "Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href=\"https://www.paypal.com/paypalme/PierreMarkuse\">PayPal me!</a>"));
        assertTrue(stringsToRemove.contains(
                "Follow me on <a href=\"https://twitter.com/Pierre_Markuse\">Twitter!</a> and <a href=\"https://mastodon.world/@pierre_markuse\">Mastodon!</a>"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "kevinmgill;4;2022-03-04T00:00:00;2022-03-03T16:58:28;https://live.staticflickr.com/65535/52723768672_2c4d7cb9f6_o_d.png;Jupiter - PJ49-77;72157664303348108=MOOOOOOOONS!!!,72157672426118932=Juno,72157715049847592=Io,72157713530464366=Hourly Cosmos;P1163=image/png,P170=Q48546,P1071=Q319,P4082=Q6314027,P571=2022-03-03T16:58:28,P577=2022-03-04T00:00,P6216=Q50423863,P275=Q19125117,P7482=Q74228490" })
    void testGetStatements(String pathAlias, int license, LocalDateTime datePosted, LocalDateTime dateTaken,
            URL assetUrl, String title, String albums, String expectedStatements) throws MalformedURLException {
        FlickrMedia media = new FlickrMedia();
        media.addMetadata(new FileMetadata());
        media.setId(new CompositeMediaId(pathAlias, ""));
        media.setLicense(license);
        media.setTitle(title);
        media.setPublicationDateTime(datePosted.atZone(ZoneId.of("UTC")));
        media.setCreationDateTime(dateTaken.atZone(ZoneId.of("UTC")));
        for (String album : albums.split(",")) {
            String[] kv = album.split("=");
            media.getPhotosets().add(new FlickrPhotoSet(Long.valueOf(kv[0]), kv[1]));
        }
        FileMetadata metadata = new FileMetadata(assetUrl);

        doCallRealMethod().when(mediaService).useMapping(any(), any(), any(), any(), any());
        doCallRealMethod().when(categorizationService).findCategoriesStatements(any(), any());

        SdcStatements statements = service.getStatements(media, metadata);

        Set<String> checkedProperties = new TreeSet<>();

        for (String expectedStatement : expectedStatements.split(",")) {
            String[] kv = expectedStatement.split("=");
            Pair<Object, Map<String, Object>> statement = statements.get(kv[0]);
            assertNotNull(statement, expectedStatement);
            if (statement.getKey() instanceof ZonedDateTime date) {
                assertEquals(kv[1] + "Z[UTC]", Objects.toString(date));
            } else {
                assertEquals(kv[1], Objects.toString(statement.getKey()));
            }
            checkedProperties.add(kv[0]);
        }

        checkedProperties.forEach(statements::remove);

        assertTrue(statements.isEmpty(), "Non-expected statements: " + statements);
    }

    @Test
    void testFindCategories() throws Exception {
        FlickrMedia media = new FlickrMedia();
        media.setPublicationYear(Year.of(2020));
        media.setId(new CompositeMediaId("kevinmgill", "52935302219"));
        media.setTitle("MSL - Sol 3841 - MastCam");
        media.setDescription("NASA/JPL-Caltech/MSSS/Kevin M. Gill");
        media.setTags(Set.of("mastcam", "curiosity", "mars", "msl", "rover"));
        media.addPhotoSet(new FlickrPhotoSet(72157651662785382L, "MSL/Curiosity"));
        media.addPhotoSet(new FlickrPhotoSet(72157713530464366L, "Hourly Cosmos"));

        doCallRealMethod().when(mediaService).useMapping(any(), any(), any(), any(), any());

        FileMetadata metadata = new FileMetadata(new URL("https://foo"));
        assertEquals(Set.of("Photos by the Curiosity rover Mastcam"), service.findCategories(media, metadata, false));
        assertEquals(Set.of("Photos by the Curiosity rover Mastcam", "Files from Kevin Gill Flickr stream",
                "Spacemedia Individuals Flickr files uploaded by null", "Spacemedia files (review needed)"),
                service.findCategories(media, metadata, true));
    }

    @Test
    void testFindLicenceTemplates() {
        FlickrMedia media = new FlickrMedia();
        media.addMetadata(new FileMetadata());
        media.setLicense(4);
        media.setId(new CompositeMediaId("pierre_markuse", "52559137901"));
        media.setTitle("Brunt Ice Shelf, Antarctica - 11 December 2022");
        media.setDescription(
                "Contains modified Copernicus Sentinel data [2022], processed by <a href=\"https://twitter.com/Pierre_Markuse\">Pierre Markuse</a>\r\n"
                        + "\r\n" + "Brunt Ice Shelf, Antarctica - 11 December 2022\r\n" + "\r\n"
                        + "Compare with 2016 image here <a href=\"https://flic.kr/p/2o5r23p\">flic.kr/p/2o5r23p</a>\r\n"
                        + "\r\n" + "Image is about 129 kilometers wide");
        assertEquals(Set.of("Flickrreview", "Attribution-Copernicus |year=2022", "Cc-by-2.0"),
                service.findLicenceTemplates(media, media.getMetadata().iterator().next()));
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public IndividualsFlickrService service(FlickrMediaRepository repository,
                @Value("${individuals.flickr.accounts}") Set<String> flickrAccounts) {
            return new IndividualsFlickrService(repository, flickrAccounts);
        }
    }
}
