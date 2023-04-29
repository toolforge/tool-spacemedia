package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.iau.IauMediaRepository;

@SpringJUnitConfig(IauServiceTest.TestConfig.class)
class IauServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private IauMediaRepository repository;

    @Autowired
    private IauService service;

    @Test
    void testReadHtml() throws Exception {
        assertEquals(
                "IauMedia [id=ann22001b, imageType=Artwork, date=2022-01-04T11:30, width=2605, height=3689, objectType=[Unspecified], objectCategories=[Illustrations], credit=IAU/GA2022, fullResMetadata=Metadata [assetUrl=https://www.iau.org/static/archives/images/original/ann22001b.tif, ], sha1=Metadata [assetUrl=https://www.iau.org/static/archives/images/large/ann22001b.jpg, ], title=Season’s Greetings from the IAUGA2022 team, description=Season’s Greetings from the IAUGA2022 team., ]",
                service
                        .newMediaFromHtml(html("iau/ann22001b.html"),
                                new URL("https://www.iau.org/public/images/detail/ann22001b/"), "ann22001b", null)
                        .toString());
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public IauService service(IauMediaRepository repository, @Value("${iau.search.link}") String searchLink) {
            return new IauService(repository, searchLink);
        }
    }
}
