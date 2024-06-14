package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.NasaSvsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.NasaSvsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsVizualisation;

@SpringJUnitConfig(NasaSvsServiceTest.TestConfig.class)
class NasaSvsServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaSvsMediaRepository repository;

    @Autowired
    private NasaSvsService service;

    @CsvSource(delimiter = ';', value = { "5274;6" })
    @ParameterizedTest
    void testMapMedia(int id, int nFiles) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NasaSvsMedia media = service.mapMedia(json("nasa/svs/" + id + ".json", NasaSvsVizualisation.class));
        assertNotNull(media);
        assertEquals(nFiles, media.getMetadataCount());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaSvsService service(NasaSvsMediaRepository repository) {
            return new NasaSvsService(repository);
        }
    }
}
