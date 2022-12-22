package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.wikimedia.commons.donvip.spacemedia.apps.SpacemediaCommonConfiguration;
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
                        Set.of("Combined Force Space Component Command", "Combined Space Operations Center")));
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
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }
    }
}
