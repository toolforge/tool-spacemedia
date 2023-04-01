package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategory;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkId;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkType;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRestrictionsRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsServiceTest.TestConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringJUnitConfig(TestConfig.class)
@TestPropertySource("/application-test.properties")
class CommonsServiceTest {

    @Autowired
    private CommonsService service;

    @MockBean
    private CommonsCategoryRepository commonsCategoryRepository;
    @MockBean
    private CommonsCategoryLinkRepository commonsCategoryLinkRepository;
    @MockBean
    private CommonsImageRepository commonsImageRepository;
    @MockBean
    private CommonsOldImageRepository commonsOldImageRepository;
    @MockBean
    private CommonsPageRepository commonsPageRepository;
    @MockBean
    private CommonsPageRestrictionsRepository commonsPageRestrictionsRepository;
    @MockBean
    private HashAssociationRepository hashAssociationRepository;
    @MockBean
    private RuntimeDataRepository runtimeDataRepository;
    @MockBean
    private RemoteService remote;

    @Test
    void testFormatWikiCode() {
        assertEquals(
                "Learn more: [http://www.spacex.com/news/2015/05/06/crew-dragon-completes-pad-abort-test www.spacex.com/news/2015/05/06/crew-dragon-completes-pad-...] and [http://www.spacex.com/news/2015/05/04/5-things-know-about-spacexs-pad-abort-test www.spacex.com/news/2015/05/04/5-things-know-about-spacex...]",
                CommonsService.formatWikiCode(
                        "Learn more: <a href=\"http://www.spacex.com/news/2015/05/06/crew-dragon-completes-pad-abort-test\" rel=\"nofollow\">www.spacex.com/news/2015/05/06/crew-dragon-completes-pad-...</a> and <a href=\"http://www.spacex.com/news/2015/05/04/5-things-know-about-spacexs-pad-abort-test\" rel=\"nofollow\">www.spacex.com/news/2015/05/04/5-things-know-about-spacex...</a>"));
    }

    @Test
    void testGetImageUrl() throws Exception {
        assertEquals(new URL("https://upload.wikimedia.org/wikipedia/commons/0/05/Now_what%3F_(232815406).jpg"),
                CommonsService.getImageUrl("Now_what?_(232815406).jpg"));
        assertEquals(new URL(
                "https://upload.wikimedia.org/wikipedia/commons/6/6c/2009-11-30_-_Chicago_Climate_Justice_activists_in_Chicago_-_Cap%27n%27Trade_protest_009.jpg"),
                CommonsService.getImageUrl(
                        "2009-11-30_-_Chicago_Climate_Justice_activists_in_Chicago_-_Cap'n'Trade_protest_009.jpg"));
        assertEquals(
                new URL("https://upload.wikimedia.org/wikipedia/commons/9/9a/%22Meillandine%22_Rose_in_clay_pot.jpg"),
                CommonsService.getImageUrl("\"Meillandine\"_Rose_in_clay_pot.jpg"));
    }

    @Test
    void testGetSubCategories() {
        mockCategoryLinks();
        assertEquals(Set.of("Combined Space Operations Center"),
                service.getSubCategories("Combined Force Space Component Command", 1));
    }

    @Test
    void testCleanupCategories() {
        mockCategoryLinks();
        assertEquals(Set.of("Spacemedia files (review needed)", "Combined Space Operations Center"),
                service.cleanupCategories(
                        Set.of("Combined Force Space Component Command", "Combined Space Operations Center"),
                        LocalDateTime.now()));
    }

    @Test
    void testMapCategoriesByDate() {
        CommonsCategory cat = new CommonsCategory();
        cat.setTitle("Bill_Nelson_in_2023");
        CommonsPage page = new CommonsPage();
        page.setTitle("Bill Nelson in 2023");
        when(commonsCategoryRepository.findByTitle("Bill_Nelson_in_2023")).thenReturn(Optional.of(cat));
        when(commonsPageRepository.findByCategoryTitle("Bill_Nelson_in_2023")).thenReturn(Optional.of(page));

        assertEquals(Set.of("Bill Nelson in 2023"),
                service.mapCategoriesByDate(Set.of("Bill Nelson"), LocalDateTime.of(2023, 1, 1, 1, 1)));
    }

    @Test
    void testIsPermittedFileType() {
        assertTrue(service.isPermittedFileType("https://www.kari.re.kr/image/kari_image_down.do?idx=79"));
        assertTrue(service.isPermittedFileType("https://photojournal.jpl.nasa.gov/archive/PIA25257.gif"));
    }

    @Test
    @Disabled("requires credentials on command line")
    void testgetWikiHtmlPreview() throws IOException {
        assertNotNull(service.getWikiHtmlPreview(
                """
                        == {{int:filedesc}} ==
                        {{NASA Photojournal
                        | catalog = PIA25257
                        | image= true
                        | video= false
                        | animation= false
                        | mission= Lunar Flashlight
                        | instrument= null
                        | caption = {{en|1=<p><center><a href="https://photojournal.jpl.nasa.gov/archive/PIA25257.gif" target="new"><img src="https://photojournal.jpl.nasa.gov/figures/PIA25257_figA_thumb.jpg" alt="Click here for Figure A for PIA25257"></a><br><b>Figure A - click on image for animation</b></center></p><p>These images show two observations of NASA's Lunar Flashlight and the private ispace HAKUTO-R Mission 1 as the two spacecraft, seen as a pair of dots, journey to the Moon. In Figure A, the images have been joined sequentially to create an animated GIF. The larger HAKUTO-R lunar lander appears as a large black dot, whereas the smaller Lunar Flashlight, which is about the size of a briefcase, appears as a fuzzy grouping of gray pixels. Stars appear as long trails.</p><p>[https://www.jpl.nasa.gov/news/nasas-lunar-flashlight-has-launched-follow-the-mission-in-real-time Both missions launched] on Dec. 11, 2022, aboard a SpaceX Falcon 9 rocket, with Lunar Flashlight and HAKUTO-R spacecraft subsequently deploying from it. Astronomer Vishnu Reddy and graduate student Adam Battle, both from University of Arizona's [https://www.lpl.arizona.edu/ Lunar and Planetary Laboratory] and [https://s4.arizona.edu/ Space4 Center], used a remote 0.5-meter (1.6-foot) telescope in Australia to track the small spacecraft. They used data from [https://ssd.jpl.nasa.gov/horizons/ the Horizons System] at NASA's Jet Propulsion Laboratory in Southern California to find their position in the sky.</p><p>These images were acquired about 39 hours after launch, when the two spacecraft were 145,000 miles (235,000 kilometers) from Earth. Black and white in the images have been inverted so that the brighter the object, the darker it is. To detect the faint reflected light from both spacecraft, they stacked 80 images, each from a 10-second exposure (for a total exposure time of 800 seconds), based on the rate of motion and direction of the spacecraft. This method resulted in stars appearing as long trails and the two spacecraft appearing as dots.</p><p>Lunar Flashlight is a small satellite mission planning to use lasers to seek out surface water ice inside permanently shadowed craters at the Moon's South Pole. The small satellite is expected to reach its science orbit around the Moon in April 2023.</p><p>For more information about the Lunar Flashlight launch, and how to follow along with the mission, go to: [https://www.jpl.nasa.gov/news/nasas-lunar-flashlight-has-launched-follow-the-mission-in-real-time https://www.jpl.nasa.gov/news/nasas-lunar-flashlight-has-launched-follow-the-mission-in-real-time]</p>}}
                        | credit= University of Arizona
                        | addition_date = 2022-12-23T17:27:49Z[Etc/UTC]
                        | globe= Moon
                        | gallery = NASA's Lunar Flashlight Spotted From Earth on Its Way to the Moon (PIA25257).tiff|TIFF versionNASA's Lunar Flashlight Spotted From Earth on Its Way to the Moon (PIA25257).gif|GIF version
                        Optional[NASA's Lunar Flashlight Spotted From Earth on Its Way to the Moon (PIA25257).tiff|TIFF version]
                        }}
                        =={{int:license-header}}==
                        {{PD-USGov-NASA}}
                        [[Category:Spacemedia files uploaded by OptimusPrimeBot]]
                        [[Category:Spacemedia files (review needed)]]
                        [[Category:Lunar Flashlight]]
                                        """,
                "PIA25257", "https://photojournal.jpl.nasa.gov/archive/PIA25257.gif"));
    }

    private void mockCategoryLinks() {
        CommonsPage page = new CommonsPage();
        page.setTitle("Combined_Space_Operations_Center");
        CommonsCategoryLinkId link = new CommonsCategoryLinkId(page, "Combined_Force_Space_Component_Command");
        when(commonsCategoryLinkRepository.findIdByTypeAndIdTo(CommonsCategoryLinkType.subcat,
                "Combined_Force_Space_Component_Command")).thenReturn(Set.of(link));
    }

    @Configuration
    @Import(SpacemediaCommonConfiguration.class)
    public static class TestConfig {
        @Bean
        public ConversionService conversionService() {
            return ApplicationConversionService.getSharedInstance();
        }

        @Bean
        public CommonsService service(@Value("${application.version}") String appVersion,
                @Value("${application.contact}") String appContact,
                @Value("${flickr4java.version}") String flickr4javaVersion,
                @Value("${spring-boot.version}") String bootVersion,
                @Value("${scribejava.version}") String scribeVersion,
                @Value("${commons.api.account}") String apiAccount,
                @Value("${commons.api.oauth1.consumer-token}") String consumerToken,
                @Value("${commons.api.oauth1.consumer-secret}") String consumerSecret,
                @Value("${commons.api.oauth1.access-token}") String accessToken,
                @Value("${commons.api.oauth1.access-secret}") String accessSecret) {
            return new CommonsService(appVersion, appContact, flickr4javaVersion, bootVersion, scribeVersion,
                    apiAccount, consumerToken, consumerSecret, accessToken, accessSecret);
        }

        @Bean
        public RestTemplateBuilder rest() {
            return new RestTemplateBuilder();
        }

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }
    }
}
