package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

/**
 * Service harvesting images from ESA JWST website.
 */
@Service
public class WebbEsaService extends AbstractOrgDjangoplicityService {

    private static final String BASE_PUBLIC_URL = "https://esawebb.org/";

    private static final String IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(BASE_PUBLIC_URL + "([a-z]+/)" + IMAGES_PATH + ".*");

    @Autowired
    public WebbEsaService(DjangoplicityMediaRepository repository,
            @Value("${webb.esa.search.link}") String searchLink) {
        super(repository, "webb.esa", searchLink);
    }

    @Override
    public String getName() {
        return "Webb (ESA)";
    }

    @Override
    public Set<String> findLicenceTemplates(DjangoplicityMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("ESA-Webb|" + media.getCredits());
        return result;
    }

    @Override
    public URL getSourceUrl(DjangoplicityMedia media, FileMetadata metadata) {
        return newURL(BASE_PUBLIC_URL + IMAGES_PATH + media.getIdUsedInOrg());
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
    protected Set<String> getEmojis(DjangoplicityMedia uploadedMedia) {
        return Set.of(Emojis.STARS);
    }

    @Override
    protected Set<String> getTwitterAccounts(DjangoplicityMedia uploadedMedia) {
        return Set.of("@ESA_Webb");
    }
}
