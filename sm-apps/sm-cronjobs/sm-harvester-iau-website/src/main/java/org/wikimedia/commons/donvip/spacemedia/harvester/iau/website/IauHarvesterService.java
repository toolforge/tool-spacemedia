package org.wikimedia.commons.donvip.spacemedia.harvester.iau.website;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.repo.eso.EsoFrontPageItem;
import org.wikimedia.commons.donvip.spacemedia.repo.eso.EsoHarvesterService;

@Service
public class IauHarvesterService extends EsoHarvesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IauHarvesterService.class);

    @Value("${iau.base.url}")
    private String iauBaseUrl;

    @Value("${iau.images.link}")
    private String iauImagesLink;

    @Override
    protected final String findDescription(Element div) {
        StringBuilder description = new StringBuilder();
        for (Element p : div.getElementById("main-page-grey").getElementsByTag("p")) {
            if (p.text().startsWith("Credit:")) {
                break;
            }
            description.append(p.html());
        }
        return description.toString();
    }

    @Override
    protected Iterator<EsoFrontPageItem> findFrontPageItems(Document document) throws IOException {
        return document.getElementById("main-page-grey").getElementsByClass("thumbnail").stream()
                .flatMap(e -> e.getElementsByTag("a").stream()).map(a -> {
                    Element img = a.getElementsByTag("img").get(0);
                    EsoFrontPageItem item = new EsoFrontPageItem();
                    try {
                        item.setId(a.attr("href").replace(iauImagesLink, "").replace("/", ""));
                        item.setUrl(a.attr("href"));
                        item.setTitle(img.attr("alt"));
                        item.setSrc(new URL(iauBaseUrl + img.attr("src")));
                    } catch (MalformedURLException ex) {
                        LOGGER.error("Cannot build thumbnail URL", ex);
                    }
                    return item;
                }).iterator();
    }
}
