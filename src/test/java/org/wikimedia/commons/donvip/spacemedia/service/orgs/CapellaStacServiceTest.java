package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

@SpringJUnitConfig(CapellaStacServiceTest.TestConfig.class)
class CapellaStacServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private StacMediaRepository repository;

    @Autowired
    private CapellaStacService service;

    @Test
    void testIsStacItemBefore() {
        assertTrue(service.isStacItemBefore(
                "../../../../capella-open-data-by-industry/capella-open-data-other/CAPELLA_C11_SM_SIDD_HH_20230927025114_20230927025119/CAPELLA_C11_SM_SIDD_HH_20230927025114_20230927025119.json",
                LocalDate.of(2023, 9, 28)));
        assertFalse(service.isStacItemBefore(
                "../../../../capella-open-data-by-industry/capella-open-data-other/CAPELLA_C11_SM_SIDD_HH_20230927025114_20230927025119/CAPELLA_C11_SM_SIDD_HH_20230927025114_20230927025119.json",
                LocalDate.of(2023, 9, 27)));
    }

    @Test
    void testSdc() {
        StacMedia media = new StacMedia();
        media.setId(new CompositeMediaId("capella-open-data", "CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146"));
        media.setCreationDateTime(ZonedDateTime.parse("2024-08-20T16:51:39.135Z"));
        media.setPublicationDateTime(ZonedDateTime.parse("2024-08-20T16:51:46.529Z"));
        media.setTitle("CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146");
        media.setUrl(newURL("https://capella-open-data.s3.us-west-2.amazonaws.com/stac/capella-open-data-by-datetime/capella-open-data-2024/capella-open-data-2024-08/capella-open-data-2024-08-20/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146.json"));
        FileMetadata fm = new FileMetadata();
        fm.setAssetUrl(newURL("https://capella-open-data.s3.amazonaws.com/data/2024/8/20/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146.tif"));
        fm.setSha1("1516c48beb65ba9a6c5a89c80d6e7404016357d0");
        fm.setSize(2082457040L);
        fm.setExtension("tif");
        fm.setOriginalFileName("CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146.tif");
        media.addMetadata(fm);

        SdcStatements sdc = service.getStatements(media, fm);

        assertEquals(
            "{P1071=(Q663611,null), P1163=(image/tiff,null), P170=(Q129698469,null), P2079=(Q725252,null), P275=(Q20007257,null), P3575=((2082457040,Q8799),null), P4082=(Q740686,null), P4092=(1516c48beb65ba9a6c5a89c80d6e7404016357d0,{P459=Q13414952}), P571=(2024-08-20T16:51:39.135Z,null), P577=(2024-08-20T16:51:46.529Z,null), P6216=(Q50423863,null), P7482=(Q74228490,{P2699=https://capella-open-data.s3.amazonaws.com/data/2024/8/20/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146.tif, P973=https://capella-open-data.s3.us-west-2.amazonaws.com/stac/capella-open-data-by-datetime/capella-open-data-2024/capella-open-data-2024-08/capella-open-data-2024-08-20/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146/CAPELLA_C15_SS_SLC_HH_20240820165132_20240820165146.json})}",
            sdc.toString());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public CapellaStacService service(StacMediaRepository repository,
                @Value("${capella.stac.catalogs}") Set<String> catalogs) {
            return new CapellaStacService(repository, catalogs);
        }
    }
}
