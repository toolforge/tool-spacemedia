package org.wikimedia.commons.donvip.spacemedia.harvester.esa.website.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.core.Harvester;

@Service
public class EsaWebsiteScrappingService implements Harvester {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsaWebsiteScrappingService.class);

    @Value("${esa.search.link}")
    private String searchLink;

    @Value("${esa.max.tries}")
    private int maxTries;

    @Value("${esa.date.pattern}")
    private String datePattern;

    @Override
    public void harvestMedia() throws IOException {
        final URL url = new URL(searchLink);
        final String proto = url.getProtocol();
        final String host = url.getHost();
        boolean moreImages = true;
        int index = 0;
        do {
            String searchUrl = searchLink.replace("<idx>", Integer.toString(index));
            try {
                boolean ok = false;
                for (int i = 0; i < maxTries && !ok; i++) {
                    try {
                        LOGGER.debug("Fetching ESA images: {}", searchUrl);
                        Document html = Jsoup.connect(searchUrl).timeout(15_000).get();
                        Elements divs = html.getElementsByClass("grid-item");
                        for (Element div : divs) {
                            URL imageUrl = new URL(proto, host, div.select("a").get(0).attr("href"));
                            index++;
                            LOGGER.debug("Checking ESA image {}: {}", index, imageUrl);
                        }
                        moreImages = !html.getElementsByClass("paging").get(0)
                                .getElementsByAttributeValue("title", "Next").isEmpty();
                        ok = true;
                    } catch (SocketTimeoutException e) {
                        LOGGER.debug(searchUrl, e);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error(searchUrl, e);
                moreImages = false;
            }
        } while (moreImages);
    }
}
