package org.wikimedia.commons.donvip.spacemedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;

class CommonsServiceTest {

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
}
