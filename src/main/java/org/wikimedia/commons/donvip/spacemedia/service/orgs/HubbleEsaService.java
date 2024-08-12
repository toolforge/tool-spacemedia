package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class HubbleEsaService extends AbstractOrgDjangoplicityService {

    private static final String HUB_BASE_PUBLIC_URL = "https://esahubble.org/";

    private static final String HUB_IMAGES_PATH = "images/";
    private static final String HUB_VIDEOS_PATH = "videos/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(HUB_BASE_PUBLIC_URL + "([a-z]+/)" + HUB_IMAGES_PATH + ".*");

    @Lazy
    @Autowired
    private HubbleNasaService nasaService;

    @Lazy
    @Autowired
    private NasaMappingService mappings;

    @Autowired
    public HubbleEsaService(DjangoplicityMediaRepository repository,
            @Value("${hubble.esa.search.link}") String searchLink) {
        super(repository, "hubble.esa", searchLink);
    }

    @Override
    public boolean updateOnProfiles(List<String> activeProfiles) {
        return super.updateOnProfiles(activeProfiles) || activeProfiles.contains("job-hubble");
    }

    @Override
    protected List<AbstractOrgService<?>> getSimilarOrgServices() {
        return List.of(nasaService);
    }

    @Override
    public String getName() {
        return "Hubble (ESA)";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia Hubble files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected SdcStatements getStatements(DjangoplicityMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata).creator("Q2513"); // Created by Hubble
        for (String instrument : media.getInstruments()) {
            wikidataStatementMapping(instrument, mappings.getNasaInstruments(), "P4082", result); // Taken with
        }
        return result;
    }

    @Override
    public Set<String> findCategories(DjangoplicityMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        replace(result, "Galaxies", "Hubble images of galaxies");
        replace(result, "Nebulae", "Hubble images of nebulae");
        replace(result, "Solar System", "Hubble Solar System images");
        replace(result, "Star clusters", "Hubble images of star clusters");
        for (String instrument : media.getInstruments()) {
            findCategoryFromMapping(instrument, "instrument", mappings.getNasaInstruments()).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(DjangoplicityMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add(media.getYear().getValue() < 2009 ? "PD-Hubble" : "ESA-Hubble");
        return result;
    }

    @Override
    public URL getSourceUrl(DjangoplicityMedia media, FileMetadata metadata, String ext) {
        return newURL(
                HUB_BASE_PUBLIC_URL + imageOrVideo(ext, HUB_IMAGES_PATH, HUB_VIDEOS_PATH) + media.getIdUsedInOrg());
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
