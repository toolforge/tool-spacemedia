package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractDjangoplicityServiceTest.TestConfig.TestAbstractDjangoplicityService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractDjangoplicityServiceTest.TestConfig.TestDjangoplicityMedia;

@SpringJUnitConfig(AbstractDjangoplicityServiceTest.TestConfig.class)
class AbstractDjangoplicityServiceTest extends AbstractAgencyServiceTest {

    @Autowired
    private TestAbstractDjangoplicityService service;

    @MockBean
    private DjangoplicityMediaRepository<TestDjangoplicityMedia> repo;

    @Test
    void testGetStatements() throws MalformedURLException {
        TestDjangoplicityMedia media = new TestDjangoplicityMedia();
        media.setDate(LocalDateTime.of(2022, 12, 19, 6, 0));
        media.setImageDimensions(new ImageDimensions(3977, 3878));
        media.setName("[KAG2008] globule 13");
        Metadata metadata = new Metadata();
        metadata.setAssetUrl(new URL("https://esahubble.org/media/archives/images/original/potw2251a.tif"));
        metadata.setPhash("57edtmbje3hvtfycdzpni9fyfal1ujv80pnmwmgwaw0h1p9nzq");
        metadata.setSha1("dde6dbb2424a7a21608302f8cad9a5b3bcccb589");

        when(wikidata.searchAstronomicalObject("[KAG2008] globule 13"))
                .thenReturn(Optional.of(Pair.of("Q86709121", null)));

        assertEquals(
                "{P1163=(image/tiff,null), P180=(Q86709121,null), P2048=((3878,Q355198),null), P2049=((3977,Q355198),null), P4092=(dde6dbb2424a7a21608302f8cad9a5b3bcccb589,{P459=Q13414952}), P571=(2022-12-19T06:00,null), P6216=(Q50423863,null), P7482=(Q74228490,{P2699=https://esahubble.org/media/archives/images/original/potw2251a.tif, P973=https://esahubble.org/images/potw2251a/}), P9310=(57edtmbje3hvtfycdzpni9fyfal1ujv80pnmwmgwaw0h1p9nzq,{P459=Q118189277})}",
                service.getStatements(media, metadata).toString());
    }

    @Configuration
    @Import(AbstractAgencyServiceTest.DefaultAgencyTestConfig.class)
    public static class TestConfig {

        static class TestDjangoplicityMedia extends DjangoplicityMedia {

        }

        static class TestAbstractDjangoplicityService extends AbstractDjangoplicityService<TestDjangoplicityMedia> {

            protected TestAbstractDjangoplicityService(
                    DjangoplicityMediaRepository<TestDjangoplicityMedia> repository) {
                super(repository, "", "", TestDjangoplicityMedia.class);
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public void updateMedia() throws IOException, UploadException {
            }

            @Override
            public URL getSourceUrl(TestDjangoplicityMedia media) throws MalformedURLException {
                return new URL("https://esahubble.org/images/potw2251a/");
            }

            @Override
            protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
                return null;
            }

            @Override
            protected String getCopyrightLink() {
                return null;
            }

            @Override
            protected Class<TestDjangoplicityMedia> getMediaClass() {
                return TestDjangoplicityMedia.class;
            }
        }

        @Bean
        public TestAbstractDjangoplicityService service(DjangoplicityMediaRepository<TestDjangoplicityMedia> repo) {
            return new TestAbstractDjangoplicityService(repo);
        }
    }
}
