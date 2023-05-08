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
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMediaRepository;

@SpringJUnitConfig(EsoServiceTest.TestConfig.class)
class EsoServiceTest extends AbstractAgencyServiceTest {

    @MockBean
    private EsoMediaRepository repository;

    @Autowired
    private EsoService service;

    @Test
    void testReadHtml() throws Exception {
        assertEquals(
                "EsoMedia [id=eso2215a, imageType=Observation, date=2022-11-10T14:00, dimensions=[width=1639, height=1682], name=Cone Nebula, NGC 2264, types=[Milky Way : Nebula : Type : Star Formation], categories=[Nebulae], credit=ESO, fullResMetadata=Metadata [assetUrl=https://cdn.eso.org/images/original/eso2215a.tif, ], metadata=Metadata [assetUrl=https://cdn.eso.org/images/large/eso2215a.jpg, ], title=ESO’s 60th anniversary image: the Cone Nebula as seen by the VLT, description=The Cone Nebula is part of a star-forming region of space, NGC 2264, about 2500 light-years away. Its pillar-like appearance is a perfect example of the shapes that can develop in giant clouds of cold molecular gas and dust, known for creating new stars. This dramatic new view of the nebula was captured with the <a href=\"https://www.eso.org/public/teles-instr/paranal-observatory/vlt/vlt-instr/fors/\">FOcal Reducer and low dispersion Spectrograph 2</a> (FORS2) instrument on ESO’s <a href=\"https://www.eso.org/public/teles-instr/paranal-observatory/vlt/\">Very Large Telescope</a> (VLT), and released on the occasion of ESO’s 60th anniversary.&nbsp;, telescopes=[FORS2, Very Large Telescope], ]",
                service.newMediaFromHtml(html("eso/eso2215a.html"),
                new URL("https://www.eso.org/public/images/eso2215a/"), "eso2215a", null).toString());
    }

    @Configuration
    @Import(DefaultAgencyTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public EsoService service(EsoMediaRepository repository, @Value("${eso.search.link}") String searchLink) {
            return new EsoService(repository, searchLink);
        }
    }
}
