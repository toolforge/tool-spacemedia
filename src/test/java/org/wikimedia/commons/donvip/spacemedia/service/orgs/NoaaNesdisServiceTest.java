package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis.NoaaNesdisMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis.NoaaNesdisMediaRepository;

@SpringJUnitConfig(NoaaNesdisServiceTest.TestConfig.class)
public class NoaaNesdisServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NoaaNesdisMediaRepository repository;

    @Autowired
    private NoaaNesdisService service;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "fanning-the-flames-goes-west-monitors-smoke-the-alisal-fire;Fanning the Flames: GOES West Monitors Smoke from the Alisal Fire;1341;1",
            "slow-moving-storms-flood-texas;Slow Moving Storms Flood Texas;748;1",
            "cyclone-yaas-makes-landfall-northeastern-india; Cyclone Yaas Makes Landfall In Northeastern India;1504;1"
    })
    void testFillMediaWithHtml(String id, String title, int descLen, int nFiles)
            throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NoaaNesdisMedia media = new NoaaNesdisMedia();
        media.setId(new CompositeMediaId("nesdis", id));
        service.fillMediaWithHtml(null, Jsoup.parse(new File("src/test/resources/noaa/nesdis/" + id + ".html")), media);
        assertEquals(title, media.getTitle());
        assertEquals(descLen, Optional.ofNullable(media.getDescription()).orElse("").length());
        assertEquals(nFiles, media.getMetadataCount());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NoaaNesdisService service(NoaaNesdisMediaRepository repository) {
            return new NoaaNesdisService(repository);
        }
    }
}
