package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.webb.WebbEsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.webb.WebbEsaMediaRepository;

/**
 * Service harvesting images from ESA JWST website.
 */
@Service
public class WebbEsaService extends AbstractDjangoplicityService<WebbEsaMedia> {

    private static final String BASE_PUBLIC_URL = "https://esawebb.org/";

    private static final String IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(BASE_PUBLIC_URL + "([a-z]+/)" + IMAGES_PATH + ".*");

    @Autowired
    public WebbEsaService(WebbEsaMediaRepository repository,
            @Value("${webb.esa.search.link}") String searchLink) {
        super(repository, "webb.esa", searchLink, WebbEsaMedia.class);
    }

    @Override
    protected Class<WebbEsaMedia> getMediaClass() {
        return WebbEsaMedia.class;
    }

    @Override
    public void updateMedia() throws IOException {
        doUpdateMedia();
    }

    @Override
    public String getName() {
        return "Webb (ESA)";
    }

    @Override
    public Set<String> findTemplates(WebbEsaMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("ESA-Webb|" + media.getCredit());
        return result;
    }

    @Override
    public URL getSourceUrl(WebbEsaMedia media) throws MalformedURLException {
        return new URL(BASE_PUBLIC_URL + IMAGES_PATH + media.getId());
    }

    @Override
    protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
        return PATTERN_LOCALIZED_URL.matcher(imgUrlLink);
    }

    @Override
    protected String getCopyrightLink() {
        return "/copyright/";
    }

    @Override
    protected String mainDivClass() {
        return "order-lg-1";
    }

    @Override
    protected String getObjectInfoH3Tag() {
        return "h4";
    }

    @Override
    protected Elements getObjectInfoTitles(Element div) {
        return div.getElementsByTag("th");
    }

    @Override
    protected Set<String> getTwitterAccounts(WebbEsaMedia uploadedMedia) {
        return Set.of("ESA_Webb");
    }
}
