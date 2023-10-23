package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaType;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgDjangoplicityServiceTest.TestConfig.TestAbstractDjangoplicityService;

@SpringJUnitConfig(AbstractOrgDjangoplicityServiceTest.TestConfig.class)
class AbstractOrgDjangoplicityServiceTest extends AbstractOrgServiceTest {

    @Autowired
    private TestAbstractDjangoplicityService service;

    @MockBean
    private DjangoplicityMediaRepository repo;

    @Test
    void testGetStatements() throws MalformedURLException {
        DjangoplicityMedia media = new DjangoplicityMedia();
        media.setPublicationDateTime(ZonedDateTime.of(2022, 12, 19, 6, 0, 0, 0, ZoneId.of("UTC")));
        media.setName("[KAG2008] globule 13");
        FileMetadata metadata = new FileMetadata();
        metadata.setImageDimensions(new ImageDimensions(3977, 3878));
        metadata.setAssetUrl(new URL("https://esahubble.org/media/archives/images/original/potw2251a.tif"));
        metadata.setPhash("57edtmbje3hvtfycdzpni9fyfal1ujv80pnmwmgwaw0h1p9nzq");
        metadata.setSha1("dde6dbb2424a7a21608302f8cad9a5b3bcccb589");

        when(wikidata.searchAstronomicalObject("[KAG2008] globule 13"))
                .thenReturn(Optional.of(Pair.of("Q86709121", null)));

        assertEquals(
                "{P1163=(image/tiff,null), P180=(Q86709121,null), P2048=((3878,Q355198),null), P2049=((3977,Q355198),null), P4092=(dde6dbb2424a7a21608302f8cad9a5b3bcccb589,{P459=Q13414952}), P571=(2022-12-19T06:00Z[UTC],null), P6216=(Q50423863,null), P7482=(Q74228490,{P2699=https://esahubble.org/media/archives/images/original/potw2251a.tif, P973=https://esahubble.org/images/potw2251a/}), P9310=(57edtmbje3hvtfycdzpni9fyfal1ujv80pnmwmgwaw0h1p9nzq,{P459=Q118189277})}",
                service.getStatements(media, metadata).toString());
    }

    @Test
    void testProcessAboutTheImage() {
        doTestProcessAboutTheImageType("Artwork", DjangoplicityMediaType.Artwork);
        doTestProcessAboutTheImageType("Annotated Photo", DjangoplicityMediaType.Annotated_Photo);
        doTestProcessAboutTheImageType("Collage; Simulation", DjangoplicityMediaType.Collage);
    }

    private void doTestProcessAboutTheImageType(String type, DjangoplicityMediaType mediaType) {
        DjangoplicityMedia media = new DjangoplicityMedia();
        service.processAboutTheImage(null, null, null, media, "Type:", null, type);
        assertEquals(mediaType, media.getImageType());
    }

    @Configuration
    @Import(AbstractOrgServiceTest.DefaultOrgTestConfig.class)
    public static class TestConfig {

        static class TestAbstractDjangoplicityService extends AbstractOrgDjangoplicityService {

            protected TestAbstractDjangoplicityService(DjangoplicityMediaRepository repository) {
                super(repository, "", "");
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public URL getSourceUrl(DjangoplicityMedia media, FileMetadata metadata) {
                return newURL("https://esahubble.org/images/potw2251a/");
            }

            @Override
            protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
                return null;
            }

            @Override
            protected String getCopyrightLink() {
                return null;
            }
        }

        @Bean
        public TestAbstractDjangoplicityService service(DjangoplicityMediaRepository repo) {
            return new TestAbstractDjangoplicityService(repo);
        }
    }
}
