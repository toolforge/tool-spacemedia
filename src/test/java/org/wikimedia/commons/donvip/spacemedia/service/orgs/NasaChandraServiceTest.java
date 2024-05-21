package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

    private static final String SRC_TEST_RESOURCES_NASA_CHANDRA = "src/test/resources/nasa/chandra/";

    @MockBean
    private NasaChandraMediaRepository repository;

    @Autowired
    private NasaChandraService service;

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "2;1999/casajph;Cassiopeia A: Chandra Maps Vital Elements in Supernovas;547;2;true;true;true",
            "1;1999/capella;Chandra Gratings Peek At Capella;2804;1;true;true;true",
            "6;1999/0237;Cassiopeia A: First Light;251;9;true;true;true",
            "3;2001/blackholes;\"Black\" Black Holes: Chandra Uncovers New Evidence For Event Horizons Surrounding Black Holes;697;2;true;true;false",
            "5;2001/m15;M15: Two X-ray Binary Systems in the Globular Cluster M15;1590;2;false;true;true",
            "1;2004/ss433;Jets Spout Far Closer to Black Hole Than Thought, Scientists Say;4886;2;true;true;true",
            "5;2005/mkn421;Mkn 421: Lost and Found: X-ray Telescope Locates Missing Matter;2358;2;true;true;true",
            "1;2006/xrf;Cosmic Blasts Much More Common, Astronomers Discover;3925;2;true;true;true",
            "5;2010/4c0058;4C+00.58: Black Hole Jerked Around Twice;3161;4;true;false;true",
            "1;2011/dcluster;NASA Telescopes Help Identify Most Distant Galaxy Cluster;4864;1;false;true;true",
            "1;2018/a2597;Cosmic Fountain Powered by Giant Black Hole;3703;4;true;true;true",
            "1;2018/gw;Neutron-star merger yields new puzzle for astrophysicists;2654;2;true;true;true",
            "3;2022/a98;Abell 98: NASA's Chandra Finds Galaxy Cluster Collision on a \"WHIM\";5352;4;true;true;true",
            "2;2022/mrk462;Mrk 462: \"Mini\" Monster Black Hole Could Hold Clues to Giant's Growth;4399;4;true;true;true",
            "3;2023/archives;A Fab Five: New Images With NASA's Chandra X-ray Observatory;9354;0;true;true;true",
            "1;2023/crabx;Historic Nebula Seen Like Never Before With NASA's IXPE;6502;2;true;true;true",
            "3;2023/ngc2264;NGC 2264: Sprightly Stars Illuminate 'Christmas Tree Cluster';4865;3;true;true;true",
            "3;2023/spiders;NASA's Chandra Catches Spider Pulsars Destroying Nearby Stars;6274;3;true;true;true",
            "4;2023/ngc253;NGC 253: Chandra Determines What Makes a Galaxy's Wind Blow;7377;3;true;false;true",
            "2;2024/ss433;SS 433: NASA's IXPE Helps Researchers Maximize 'Microquasar' Findings;3319;3;true;true;true",
            "3;2024/football;Sagittarius A*: Telescopes Show the Milky Way's Black Hole is Ready for a Kick;5561;3;true;true;true",
            "6;2024/timelapse;NASA's Chandra Releases Doubleheader of Blockbuster Hits;7327;3;true;true;true"
    })
    void testFillMediaWithHtml(int nMedia, String id, String title, int descLen, int nFiles, boolean checkCredits,
            boolean checkDescription, boolean checkPublicationDate) throws IOException {
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        NasaChandraMedia media = new NasaChandraMedia();
        media.setId(new CompositeMediaId("chandra", id));
        List<NasaChandraMedia> medias = service.fillMediaWithHtml("https://chandra.si.edu/photo/" + id,
                Jsoup.parse(new File(SRC_TEST_RESOURCES_NASA_CHANDRA + id.replace('/', '_') + ".htm")), media);
        assertEquals(nMedia, medias.size());
        for (NasaChandraMedia m : medias) {
            assertNotNull(m.getTitle());
            if (checkCredits) {
                assertNotNull(m.getCredits());
            }
            if (checkDescription) {
                assertNotNull(m.getDescription());
            }
            if (checkPublicationDate) {
                assertNotNull(m.getPublicationDate());
            }
            if (nFiles > 0) {
                assertTrue(m.hasMetadata());
            }
        }
        assertEquals(title, media.getTitle());
        assertEquals(descLen, ofNullable(media.getDescription()).orElse("").length());
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
            return new NasaChandraService(repository) {
                @Override
                protected Document getMore(String id, String url) throws IOException {
                    return Jsoup.parse(new File(SRC_TEST_RESOURCES_NASA_CHANDRA + id.replace('/', '_') + "_more.htm"));
                }
            };
        }
    }
}
