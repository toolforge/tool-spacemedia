package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class HubbleEsaService extends AbstractOrgDjangoplicityService {

    private static final String HUB_BASE_PUBLIC_URL = "https://esahubble.org/";

    private static final String HUB_IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(HUB_BASE_PUBLIC_URL + "([a-z]+/)" + HUB_IMAGES_PATH + ".*");

    @Autowired
    public HubbleEsaService(DjangoplicityMediaRepository repository,
            @Value("${hubble.esa.search.link}") String searchLink) {
        super(repository, "hubble.esa", searchLink);
    }

    @Override
    public String getName() {
        return "Hubble (ESA)";
    }

    @Override
    public Set<String> findCategories(DjangoplicityMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        replace(result, "Galaxies", "Hubble images of galaxies");
        replace(result, "Nebulae", "Hubble images of nebulae");
        replace(result, "Solar System", "Hubble Solar System images");
        replace(result, "Star clusters", "Hubble images of star clusters");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(DjangoplicityMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        if (media.getYear().getValue() < 2009) {
            result.add("PD-Hubble");
        } else {
            result.add("ESA-Hubble");
        }
        return result;
    }

    @Override
    public URL getSourceUrl(DjangoplicityMedia media, FileMetadata metadata) {
        return newURL(HUB_BASE_PUBLIC_URL + HUB_IMAGES_PATH + media.getIdUsedInOrg());
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
    protected Set<String> getEmojis(DjangoplicityMedia uploadedMedia) {
        return Set.of(Emojis.STARS);
    }

    @Override
    protected Set<String> getTwitterAccounts(DjangoplicityMedia uploadedMedia) {
        return Set.of("@HUBBLE_space");
    }
}
