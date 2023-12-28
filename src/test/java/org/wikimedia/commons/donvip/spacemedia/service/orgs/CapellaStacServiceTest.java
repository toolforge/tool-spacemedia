package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.osm.NominatimService;

@SpringJUnitConfig(CapellaStacServiceTest.TestConfig.class)
class CapellaStacServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private StacMediaRepository repository;

    @MockBean
    private NominatimService nominatim;

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
