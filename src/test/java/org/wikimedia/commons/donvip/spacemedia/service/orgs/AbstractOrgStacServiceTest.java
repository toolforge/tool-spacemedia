package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;

@SpringJUnitConfig(AbstractOrgStacServiceTest.TestConfig.class)
class AbstractOrgStacServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private StacMediaRepository repository;

    @Autowired
    private AbstractOrgStacService service;

    @ParameterizedTest
    @CsvSource({ "CAPELLA_C09_SM_GEC_VV_20231118132210_20231118132214",
            "CAPELLA_C14_SP_SLC_VV_20240414014317_20240414014346" })
    void testFetchMedia(String id) throws MalformedURLException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        assertNotNull(service.fetchStacMedia("capella",
                Path.of("src/test/resources/capella/" + id + ".json").toUri().toURL()));
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public AbstractOrgStacService service(StacMediaRepository repository) {
            return new AbstractOrgStacService(repository, "test", Set.of()) {

                @Override
                public String getName() {
                    return "test";
                }

                @Override
                protected void enrichStacMedia(StacMedia media, StacItem item) {
                    // Do nothing
                }

                @Override
                protected boolean isStacItemBefore(String itemHref, LocalDate doNotFetchEarlierThan) {
                    return false;
                }

                @Override
                protected boolean isStacItemIgnored(String itemHref) {
                    return false;
                }

                @Override
                protected String hiddenUploadCategory(String repoId) {
                    return "";
                }
            };
        }
    }
}
