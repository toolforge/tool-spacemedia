package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.website.NasaWebsiteMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.website.NasaWebsiteMediaRepository;

@SpringJUnitConfig(NasaWebsiteServiceTest.TestConfig.class)
@TestPropertySource("/application-test.properties")
class NasaWebsiteServiceTest extends AbstractOrgServiceTest {

    @MockBean
    private NasaWebsiteMediaRepository repository;

    @Autowired
    private NasaWebsiteService service;

    @CsvSource(delimiter = '|', value = {
            "image-article|artemis-ii-water-deluge-test|Artemis II Water Deluge Test|741|https://www.nasa.gov/wp-content/uploads/2023/10/ksc-20231024-ph-kls01-0066orig.jpg?w=2048|NASA / Kim Shiflett|2023-10-26T17:49:46Z|1|https://www.nasa.gov/wp-content/uploads/2023/10/ksc-20231024-ph-kls01-0066orig.jpg|0|0|[]|",
            "image-article|early-production-continues-on-advanced-upper-stage-for-nasa-moon-rocket|Early Production Continues on Advanced Upper Stage for NASA Moon Rocket|295|https://www.nasa.gov/wp-content/uploads/2023/11/maf-20231024-eus-loxbreakover09.jpg|NASA/Michael DeMocker|2023-11-03T15:17:58-04:00|1|https://www.nasa.gov/wp-content/uploads/2023/11/maf-20231024-eus-loxbreakover09.jpg|8192|5462|[Marshall Space Flight Center, Michoud Assembly Facility, Artemis, Space Launch System (SLS)]|2023-10-24",
            "image-article|just-in-time-for-halloween-nasas-juno-mission-spots-eerie-face-on-jupiter|Just in Time for Halloween, NASA’s Juno Mission Spots Eerie “Face” on Jupiter|1504|https://www.nasa.gov/wp-content/uploads/2023/10/just-in-time-for-halloween-nasas-juno-mission-spots-eerie-face-on-jupiter.jpg?w=1170|Image data: NASA/JPL-Caltech/SwRI/MSSS Image processing by Vladimir Tarasov © CC BY|2023-10-25T21:36:53Z|1|https://www.nasa.gov/wp-content/uploads/2023/10/just-in-time-for-halloween-nasas-juno-mission-spots-eerie-face-on-jupiter.jpg|0|0|[]|",
            "image-article|nasas-lucy-spacecraft-discovers-2nd-asteroid-during-dinkinesh-flyby|NASA’s Lucy Spacecraft Discovers 2nd Asteroid During Dinkinesh Flyby|4065|https://www.nasa.gov/wp-content/uploads/2023/11/dinkinesh-firstlook-llorri.png?w=1000|NASA/Goddard/SwRI/Johns Hopkins APL/NOIRLab|2023-11-02T18:06:51Z|2|https://www.nasa.gov/wp-content/uploads/2023/11/dinkinesh-firstlook-llorri.png|0|0|[]|",
            "image-article|nasa-hq-juneteenth-flag-raising-ceremony|NASA HQ Juneteenth Flag Raising Ceremony|462|https://www.nasa.gov/wp-content/uploads/2023/07/52977480320_a215d08bcc_o.jpg?w=1041|NASA/Keegan Barber|2023-06-16T14:23Z|1|https://www.nasa.gov/wp-content/uploads/2023/07/52977480320_a215d08bcc_o.jpg|0|0|[]|",
            "image-article|an-aurora-in-another-light|An Aurora in Another Light|894|https://www.nasa.gov/wp-content/uploads/2024/01/nameraurora-vir-2023309-lrg.jpg?w=1943|NASA/Lauren Dauphin and Wanmei Liang; NOAA|2024-01-17T19:55:24Z|1|https://www.nasa.gov/wp-content/uploads/2024/01/nameraurora-vir-2023309-lrg.jpg|0|0|[]|",
            "image-article|nasas-wallops-c-130-plays-vital-role-in-successful-parachute-airdrop-test|NASA’s Wallops C-130 Plays Vital Role in Successful Parachute Airdrop Test|1473|https://www.nasa.gov/wp-content/uploads/2024/01/wallops-c130-ccpat-2024.jpg?w=2048|U.S. Army Yuma Proving Ground|2024-01-12T19:16:28Z|1|https://www.nasa.gov/wp-content/uploads/2024/01/wallops-c130-ccpat-2024.jpg|0|0|[]|",
            "image-article|nasa-teams-prepare-moon-rocket-to-spacecraft-connector-for-assembly|NASA Teams Prepare Moon Rocket-to-Spacecraft Connector for Assembly|1967|https://www.nasa.gov/wp-content/uploads/2023/12/msfc-11292023-bldg-4708-osa-ii-flip-and-diaphragm-install-1.jpg?w=2048|NASA/Sam Lott|2023-12-11T00:00:00Z|1|https://www.nasa.gov/wp-content/uploads/2023/12/msfc-11292023-bldg-4708-osa-ii-flip-and-diaphragm-install-1.jpg|0|0|[]|",
            "image-article|vast-and-rich-studying-the-ocean-with-nasa-computer-simulations|‘Vast and Rich:’ Studying the Ocean With NASA Computer Simulations|1459||NASA/Bron Nelson, David Ellsworth|2024-04-22T00:00:00Z|1|https://www.nasa.gov/wp-content/uploads/2024/04/scientists-explore-ocean-currents-through-supercomputer-simulations.mp4|0|0|[]|",
            "image-article|collins-aerospace-tests-nasa-space-station-suit-in-weightlessness|Collins Aerospace Tests NASA Space Station Suit in Weightlessness|3037||Collins Aerospace|2024-02-12T00:00:00Z|3|https://www.nasa.gov/wp-content/uploads/2024/02/collins-0g-6.jpg|0|0|[]|",
            "image-detail|53297865145-e3db450a1b-o|Details from Webb’s Cameras Reveal Crabby Composition|284|https://www.nasa.gov/wp-content/uploads/2023/11/53297865145-e3db450a1b-o.png|NASA, ESA, CSA, STScI, T. Temim (Princeton University)|2023-11-02T11:45:27-04:00|1|https://www.nasa.gov/wp-content/uploads/2023/11/53297865145-e3db450a1b-o.png|10509|9151|[]|2023-10-30",
            "image-detail|five-objects-at-various-distances-that-have-been-observed-by-chandra|Five objects at various distances that have been observed by Chandra|518|https://www.nasa.gov/wp-content/uploads/2015/02/iyl_lg.jpg||2023-01-06T13:23:51-05:00|1|https://www.nasa.gov/wp-content/uploads/2015/02/iyl_lg.jpg|3240|4020|[WebMod Grandfathered]|",
            "image-detail|hubble-jupiter-jul22-3-flat-final|Jupiter in Ultraviolet|69|https://www.nasa.gov/wp-content/uploads/2023/11/hubble-jupiter-jul22-3-flat-final.webp|NASA, ESA, and M. Wong (University of California - Berkeley); Processing: Gladys Kober (NASA/Catholic University of America)|2023-11-03T12:24:52-04:00|1|https://www.nasa.gov/wp-content/uploads/2023/11/hubble-jupiter-jul22-3-flat-final.webp|1328|1285|[]|",
            "image-detail|amf-ed07-0026-02|Range safety and phased-array range user system antennas validated in the ECANS project can be seen just behind the cockpit on NASA’s NF-15B research aircraft.|159|https://images-assets.nasa.gov/image/ED07-0026-02/ED07-0026-02~large.jpg?w=1920&h=1308&fit=clip&crop=faces%2Cfocalpoint|AFRC|2007-02-26T00:00:00Z|1|https://images-assets.nasa.gov/image/ED07-0026-02/ED07-0026-02~large.jpg|1920|1308|[]|2007-02-26",
            "image-detail|4_chaderjian_n_fig_01_sc13_sim_only|4_chaderjian_n_fig_01_sc13_sim_only|0|https://www.nasa.gov/wp-content/uploads/2015/03/4_chaderjian_n_fig_01_sc13_sim_only.png|NASA / Jasim Ahmad and Neal Chaderjian|2023-03-06T16:08:06-05:00|1|https://www.nasa.gov/wp-content/uploads/2015/03/4_chaderjian_n_fig_01_sc13_sim_only.png|1600|1200|[WebMod Grandfathered]|",
            "image-detail|sts-53-2|sts-53|0|https://www.nasa.gov/wp-content/uploads/2023/09/sts-53.jpeg||2023-09-01T00:00Z|1|https://www.nasa.gov/wp-content/uploads/2023/09/sts-53.jpeg|639|639|[]||",
            "video-detail|jpl-20210328-psychef-0001-nasas-psyche-final-assembly-beginsorig-1|NASA’s Psyche: Final Assembly Begins|849|||2023-09-05T14:06:44-04:00[UTC-04:00]|1|https://smd-cms.nasa.gov/wp-content/uploads/2023/09/jpl-20210328-psychef-0001-nasas-psyche-final-assembly-beginsorig-1.mp4|1920|1080|[]|2021-03-29",
            "video-detail|ksc-20220429-mh-csh01-0001-psyche-arrival-3302118large|Psyche Spacecraft Arrival|173|||2023-09-05T15:43:37-04:00[UTC-04:00]|1|https://smd-cms.nasa.gov/wp-content/uploads/2023/09/ksc-20220429-mh-csh01-0001-psyche-arrival-3302118large.mp4|1920|1080|[]|" })
    @ParameterizedTest
    void testReadHtml(String repo, String id, String title, int descriptionLength, URL thumbnailUrl,
            String credits, ZonedDateTime publicationDate, int numberOfFiles, URI firstAssetUri, int width, int height,
            String tags, LocalDate creationDate) throws Exception {
        NasaWebsiteMedia media = new NasaWebsiteMedia();
        media.setId(new CompositeMediaId(repo, id));
        when(metadataRepository.save(any(FileMetadata.class))).thenAnswer(a -> a.getArgument(0, FileMetadata.class));
        when(commonsService.isPermittedFileUrl(any())).thenReturn(true);

        service.fillMediaWithHtml("https://www.nasa.gov/" + repo + '/' + id,
                Jsoup.parse(new File("src/test/resources/nasa/website/" + repo + '/' + id + ".htm")), null, media);

        assertEquals(title, media.getTitle());
        if (descriptionLength == 0) {
            assertNull(media.getDescription());
        } else {
            assertEquals(descriptionLength, media.getDescription().length());
        }
        assertEquals(thumbnailUrl, media.getThumbnailUrl());
        assertEquals(credits, media.getCredits());
        if (publicationDate.toString().endsWith("T00:00Z")) {
            assertEquals(publicationDate.toLocalDate(), media.getPublicationDate());
        } else {
            assertEquals(publicationDate, media.getPublicationDateTime());
        }
        assertEquals(numberOfFiles, media.getMetadata().size());
        FileMetadata fm = media.getMetadata().iterator().next();
        assertEquals(firstAssetUri, fm.getAssetUri());
        if (width > 0 || height > 0) {
            ImageDimensions dims = fm.getImageDimensions();
            assertEquals(width, dims.getWidth());
            assertEquals(height, dims.getHeight());
        }
        assertEquals(tags, media.getKeywords().toString());
        assertEquals(creationDate, media.getCreationDate());
    }

    @Configuration
    @Import(DefaultOrgTestConfig.class)
    static class TestConfig {

        @Bean
        @Autowired
        public NasaWebsiteService service(NasaWebsiteMediaRepository repository) {
            return new NasaWebsiteService(repository);
        }
    }
}
