package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra.NasaChandraMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra.NasaChandraMediaRepository;

@SpringJUnitConfig(NasaChandraServiceTest.TestConfig.class)
class NasaChandraServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaChandraMediaRepository repository;

    @Autowired
    private NasaChandraService service;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "1999/casajph;Cassiopeia A: Chandra Maps Vital Elements in Supernovas;547;2;true;true",
            "1999/capella;Chandra Gratings Peek At Capella;2804;1;true;true",
            "1999/0237;Cassiopeia A: First Light;251;9;true;true",
            "2001/blackholes;\"Black\" Black Holes: Chandra Uncovers New Evidence For Event Horizons Surrounding Black Holes;697;2;true;false",
            "2005/mkn421;Mkn 421: Lost and Found: X-ray Telescope Locates Missing Matter;2358;2;true;true",
            "2006/xrf;Cosmic Blasts Much More Common, Astronomers Discover;3925;2;true;true",
            "2011/dcluster;NASA Telescopes Help Identify Most Distant Galaxy Cluster;4864;1;false;true",
            "2018/a2597;Cosmic Fountain Powered by Giant Black Hole;3703;4;true;true",
            "2018/gw;Neutron-star merger yields new puzzle for astrophysicists;2654;2;true;true",
            "2023/crabx;Historic Nebula Seen Like Never Before With NASA's IXPE;6502;2;true;true",
            "2023/ngc2264;NGC 2264: Sprightly Stars Illuminate 'Christmas Tree Cluster';4865;3;true;true",
            "2024/football;Sagittarius A*: Telescopes Show the Milky Way's Black Hole is Ready for a Kick;5561;3;true;true"
    })
    void testFillMediaWithHtml(String id, String title, int descLen, int nFiles, boolean checkCredits,
            boolean checkPublicationDate) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NasaChandraMedia media = new NasaChandraMedia();
        media.setId(new CompositeMediaId("chandra", id));
        service.fillMediaWithHtml("https://chandra.si.edu/photo/" + id,
                Jsoup.parse(new File("src/test/resources/nasa/chandra/" + id.replace('/', '_') + ".htm")),
                media);
        assertEquals(title, media.getTitle());
        assertEquals(descLen, Optional.ofNullable(media.getDescription()).orElse("").length());
        assertEquals(nFiles, media.getMetadataCount());
        assertEquals(nFiles, media.getMetadataStream().map(FileMetadata::getAssetUri).distinct().count());
        if (checkCredits) {
            assertNotNull(media.getCredits());
        }
        assertNotNull(media.getDescription());
        if (checkPublicationDate) {
            assertNotNull(media.getPublicationDate());
        }
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaChandraService service(NasaChandraMediaRepository repository) {
            return new NasaChandraService(repository);
        }
    }
}
