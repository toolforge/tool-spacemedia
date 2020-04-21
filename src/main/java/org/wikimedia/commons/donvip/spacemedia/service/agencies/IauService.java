package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoFrontPageItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.iau.IauMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.iau.IauMediaRepository;

@Service
public class IauService extends CommonEsoService<IauMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IauService.class);

    private static final String IAU_BASE_URL = "https://www.iau.org";

    private static final String IAU_PUBLIC_PATH = "/public/";

    private static final String IAU_BASE_PUBLIC_URL = IAU_BASE_URL + IAU_PUBLIC_PATH;

    private static final String IAU_IMAGES_PATH = "images/detail/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(IAU_BASE_PUBLIC_URL + "([a-z]+/)" + IAU_IMAGES_PATH + ".*");

    @Autowired
    public IauService(IauMediaRepository repository, @Value("${iau.search.link}") String searchLink) {
        super(repository, searchLink, IauMedia.class);
    }

    @Override
    protected Class<IauMedia> getMediaClass() {
        return IauMedia.class;
    }

    @Override
    @Scheduled(fixedRateString = "${iau.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
        doUpdateMedia();
    }

    @Override
    protected Iterator<EsoFrontPageItem> findFrontPageItems(Document document) throws IOException {
        return document.getElementById("main-page-grey").getElementsByClass("thumbnail").stream()
                .flatMap(e -> e.getElementsByTag("a").stream()).map(a -> {
                    Element img = a.getElementsByTag("img").get(0);
                    EsoFrontPageItem item = new EsoFrontPageItem();
                    try {
                        item.setId(a.attr("href").replace(IAU_PUBLIC_PATH + IAU_IMAGES_PATH, "").replace("/", ""));
                        item.setUrl(a.attr("href"));
                        item.setTitle(img.attr("alt"));
                        item.setSrc(new URL(IAU_BASE_URL + img.attr("src")));
                    } catch (MalformedURLException ex) {
                        LOGGER.error("Cannot build thumbnail URL", ex);
                    }
                    return item;
                }).iterator();
    }

    @Override
    protected StringBuilder findDescription(Element div) {
        StringBuilder description = new StringBuilder();
        for (Element p : div.getElementById("main-page-grey").getElementsByTag("p")) {
            if (p.text().startsWith("Credit:")) {
                break;
            }
            description.append(p.html());
        }
        return description;
    }

    @Override
    protected String getObjectInfoClass() {
        return "col-md-3";
    }

    @Override
    protected String getObjectInfoTitleClass() {
        return "info-list-title";
    }

    @Override
    protected Collection<String> getForbiddenCategories() {
        // General permission does not extend to use of the IAU's logo,
        // which shall remain protected and may not be used or reproduced without prior
        // and individual written consent of the IAU.
        return Arrays.asList("IAU Logos");
    }

    @Override
    public String getName() {
        return "IAU";
    }

    @Override
    public List<String> findTemplates(IauMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("IAU");
        return result;
    }

    @Override
    public URL getSourceUrl(IauMedia media) throws MalformedURLException {
        return new URL(IAU_BASE_PUBLIC_URL + IAU_IMAGES_PATH + media.getId());
    }

    @Override
    protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
        return PATTERN_LOCALIZED_URL.matcher(imgUrlLink);
    }

    @Override
    protected String getCopyrightLink() {
        return "/copyright/";
    }
}
