package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.regex.Pattern.compile;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.IgnoreCriteria;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.IgnoreCriteriaOnRepoId;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.IgnoreCriteriaOnTerms;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

@Service
public class NasaFlickrService extends AbstractOrgFlickrService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaFlickrService.class);

    public static final List<String> STRINGS_TO_REMOVE = List.of(
            "Find us on Instagram"
    );

    public static final List<Pattern> PATTERNS_TO_REMOVE = List.of(
            compile("(?:<b>)?<a href=\"[^\"]+\"(?: rel=\"nofollow\")?>(?:Find|Follow|Join|Like) us on .+</a>(?:</b>)?"),
            compile("(?:<b>)?<a href=\"[^\"]+\"(?: rel=\"nofollow\")?>.+ (?:Image|Media|Video) Use Policy</a>(?:</b>)?"),
            compile("(?:<b>)?<a href=\"[^\"]+\"(?: rel=\"nofollow\")?>Subscribe to .+</a>(?:</b>)?"),
            compile("(?:<b>)?<a href=\"[^\"]+\"(?: rel=\"nofollow\")?>NASA Goddard Space Flight Center</a>(?:</b>)? is home to the nation's largest organization of combined scientists, engineers and technologists that build spacecraft, instruments and new technology to study the Earth, the sun, our solar system, and the universe."),
            compile("(?:<b>)?<a href=\"[^\"]+\"(?: rel=\"nofollow\")?>NASA Goddard Space Flight Center</a>(?:</b>)? enables NASA’s mission through four scientific endeavors: Earth Science, Heliophysics, Solar System Exploration, and Astrophysics. Goddard plays a leading role in NASA’s accomplishments by contributing compelling scientific knowledge to advance the Org’s mission."),
            compile("(?:<b>)?(?:Find|Follow|Join|Like) us on <a href=\"[^\"]+\"(?: rel=\"nofollow\")?>.+</a>(?:</b>)?"),
            compile("(?:<b>)?(?:Find|Follow|Join|Like) us on <a href=\"[^\"]+\"(?: rel=\"nofollow\")?>.+</a>(?:, <a href=\"[^\"]+\"(?: rel=\"nofollow\")?>.+</a>)* and <a href=\"[^\"]+\"(?: rel=\"nofollow\")?>.+</a>(?:</b>)?"),
            compile("(?:<b>)?(?:Find|Follow|Join|Like) us on .+ ( <a href=\"[^\"]+\"(?: rel=\"nofollow\")?>.+</a> )(?:</b>)?")
    );

    @Autowired
    public NasaFlickrService(FlickrMediaRepository repository,
            @Value("${nasa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "nasa.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "NASA (Flickr)";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia NASA Flickr files uploaded by " + commonsService.getAccount();
    }

    @Override
    public String getUiRepoId(String repoId) {
        // Remove "nasa" from ids displayed in UI to make the long list fit in screen
        return super.getUiRepoId(repoId).replaceAll("nasa[_-]?", "").replaceAll("_?photos?", "").replace("mission", "")
                .replace("telescope", "").replace("-science", "").replace("-mars", "").replace("-infrared-sounder", "")
                .replace("researchstation", "").replace("lander", "").replace("project", "").replace("archive", "");
    }

    @Override
    protected boolean includeAllLicences() {
        return true; // A lot of NASA employees seem not aware their work is public domain :(
    }

    @Override
    protected List<IgnoreCriteria> getIgnoreCriteria() {
        List<IgnoreCriteria> result = super.getIgnoreCriteria();
        result.add(new IgnoreCriteriaOnTerms(List.of("Copyright 2010 Mars Institute"), List.of("Photo by", "(NASA")));
        result.add(new IgnoreCriteriaOnRepoId(List.of("nasa-jpl"), List.of("NASA")));
        return result;
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            try {
                result.add("Files from " + switch (media.getPathAlias()) {
                case "atmospheric-infrared-sounder" -> "Atmospheric Infrared Sounder";
                case "ghrcdaac" -> "NASA Global Hydrology Resource Center DAAC";
                case "nasawebbtelescope" -> "the James Webb Space Telescope";
                case "40054892@N06" -> "NASA Analogs";
                case "nasa_appel" -> "NASA APPEL Knowledge Services";
                case "nasadfrc" -> "NASA Dryden Flight Research Center";
                case "nasaearthobservatory" -> "NASA Earth Observatory";
                case "earthrightnow" -> "NASA Earth Right Now";
                case "nasaedge" -> "NASA EDGE";
                case "nasafo" -> "NASA Flight Opportunities";
                case "nasaglenn" -> "NASA Glenn Research Center";
                case "gsfc", "nasa_goddard" -> "NASA Goddard Space Flight Center";
                case "nasahqphoto" -> "NASA HQ";
                case "nasahubble" -> "Hubble Space Telescope";
                case "nasa_ice" -> "NASA ICE";
                case "nasa2explore" -> "NASA Johnson";
                case "nasa_jsc_photo" -> "JSC Office of STEM Engagement";
                case "nasa_larss" -> "NASA LARSS";
                case "nasacommons" -> "NASA on The Commons";
                case "nasampcv", "nasaorion" -> "NASA Orion";
                case "nasaspaceflightawareness" -> "NASA Space Flight Awareness";
                case "nasa_langley", "larc-science" -> "NASA Langley";
                case "nasamarshall", "nasamarshallphotos" -> "NASA Marshall Space Flight Center";
                case "nasaarmstrong" -> "NASA Armstrong";
                case "nasablueshift" -> "NASA Universe";
                case "nasacolab" -> "NASA CoLab";
                case "nasa-jpl" -> "Jet Propulsion Laboratory";
                case "nasakennedy" -> "NASA Kennedy";
                case "nasarobonaut" -> "NASA Robonaut";
                case "oursolarsystem" -> "NASA Solar System Exploration";
                case "nasa_hsf" -> "HSF Committee";
                case "sdomission" -> "Solar Dynamics Observatory";
                case "uahirise-mars" -> "UAHiRISE";
                case "43066628@N07" -> "NASA X";
                case "nasadesertrats" -> "NASA Desert RATS";
                case "hmpresearchstation" -> "Haughton Mars Project Research Station";
                case "morpheuslander" -> "NASA Project Morpheus";
                default -> throw new IllegalStateException("Unsupported account: " + media.getPathAlias());
                } + " Flickr stream");
            } catch (IllegalStateException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return result;
    }

    @Override
    protected String getNonFreeLicenceTemplate(FlickrMedia media) {
        return "PD-USGov-NASA";
    }

    @Override
    protected Collection<String> getStringsToRemove(FlickrMedia media) {
        return STRINGS_TO_REMOVE;
    }

    @Override
    protected Collection<Pattern> getPatternsToRemove(FlickrMedia media) {
        return PATTERNS_TO_REMOVE;
    }

    @Override
    protected String getSource(FlickrMedia media, FileMetadata metadata) {
        String result = super.getSource(media, metadata);
        // Extract a NASA id we could search on images.nasa.gov
        String nasaId = media.getUserDefinedId().orElse(null);
        if (nasaId != null) {
            result += "\n{{NASA-image|id=" + nasaId + "|center=" + center(media) + "}}";
        }
        return result;
    }

    private static String center(FlickrMedia media) {
        // https://commons.wikimedia.org/wiki/Template:NASA-image#Table_of_identification_codes
        switch (media.getPathAlias()) {
        case "nasa-jpl", "uahirise-mars", "atmospheric-infrared-sounder", "earthrightnow", "oursolarsystem":
            return "JPL";
        case "nasamarshall", "nasamarshallphotos", "ghrcdaac":
            return "MSFC";
        case "gsfc", "nasa_goddard", "nasahubble", "nasawebbtelescope", "nasaearthobservatory", "sdomission":
            return "GSFC";
        case "nasadfrc":
            return "DFRC";
        case "nasaarmstrong", "nasafo":
            return "AFRC";
        case "nasa_langley", "larc-science", "nasaedge":
            return "LARC";
        case "nasaglenn":
            return "GRC";
        case "nasa_jsc_photo", "nasa2explore", "nasarobonaut", "morpheuslander":
            return "JSC";
        case "nasacolab", "hmpresearchstation":
            return "AMES";
        case "nasakennedy":
            return "KSC";
        case "nasahqphoto":
            return "HQ";
        default:
            return "";
        }
    }

    @Override
    public Set<String> findAfterInformationTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findAfterInformationTemplates(media, metadata);
        if ("uahirise-mars".equals(media.getPathAlias())) {
            result.add("NASA Photojournal/attribution|class=MRO|mission=MRO|name=MRO|credit=HiRISE");
        }
        return result;
    }

    @Override
    protected SdcStatements getStatements(FlickrMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        if ("uahirise-mars".equals(media.getPathAlias())) {
            result.creator("Q183160") // Created by MRO
                    .depicts("Q111") // Depicts Mars
                    .locationOfCreation("Q111") // Created in Mars orbit
                    .fabricationMethod(Q725252_SATELLITE_IMAGERY)
                    .capturedWith("Q1036092"); // Taken with HiRISE
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }
}
