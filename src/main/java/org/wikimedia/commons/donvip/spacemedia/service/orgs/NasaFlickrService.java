package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    private static final List<String> STRINGS_TO_REMOVE = List.of(
            "<a href=\"http://instagram.com/NASAWebbTelescp\" rel=\"nofollow\">Follow us on Instagram</a>",
            "<a href=\"http://plus.google.com/+NASAWebbTelescope\" rel=\"nofollow\">Follow us on Google Plus</a>",
            "<a href=\"http://twitter.com/#!/NASAWebbTelescp\" rel=\"nofollow\">Follow us on Twitter</a>",
            "<a href=\"http://www.facebook.com/webbtelescope\" rel=\"nofollow\">Like us on Facebook</a>",
            "<a href=\"http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html\" rel=\"nofollow\">NASA Image Use Policy</a>",
            "<a href=\"http://www.youtube.com/nasawebbtelescope\" rel=\"nofollow\">Subscribe to our YouTube channel</a>",
            "<b><a href=\"http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html\">NASA image use policy.</a></b>",
            "<b><a href=\"http://www.nasa.gov/audience/formedia/features/MP_Photo_Guidelines.html\" rel=\"nofollow\">NASA image use policy.</a></b>",
            "<b><a href=\"http://www.nasa.gov/centers/goddard/home/index.html\" rel=\"nofollow\">NASA Goddard Space Flight Center</a></b> enables NASA’s mission through four scientific endeavors: Earth Science, Heliophysics, Solar System Exploration, and Astrophysics. Goddard plays a leading role in NASA’s accomplishments by contributing compelling scientific knowledge to advance the Org’s mission.",
            "<b><a href=\"https://www.nasa.gov/centers/goddard/home/index.html\">NASA Goddard Space Flight Center</a></b> is home to the nation's largest organization of combined scientists, engineers and technologists that build spacecraft, instruments and new technology to study the Earth, the sun, our solar system, and the universe.",
            "<b><a href=\"http://www.nasa.gov/centers/goddard/home/index.html\">NASA Goddard Space Flight Center</a></b> enables NASA’s mission through four scientific endeavors: Earth Science, Heliophysics, Solar System Exploration, and Astrophysics. Goddard plays a leading role in NASA’s accomplishments by contributing compelling scientific knowledge to advance the Org’s mission.",
            "<b>Find us on <a href=\"http://instagrid.me/nasagoddard/?vm=grid\" rel=\"nofollow\">Instagram</a></b>",
            "<b>Find us on <a href=\"http://instagrid.me/nasagoddard/?vm=grid\">Instagram</a></b>",
            "<b>Find us on <a href=\"http://www.instagram.com/nasagoddard/?vm=grid\">Instagram</a></b>",
            "<b>Find us on <a href=\"https://twitter.com/NASAHubble\">Twitter</a>, <a href=\"https://instagram.com/nasahubble?utm_medium=copy_link\">Instagram</a>, <a href=\"https://www.facebook.com/NASAHubble\">Facebook</a> and <a href=\"https://www.youtube.com/playlist?list=PL3E861DC9F9A8F2E9\"> YouTube</a></b>",
            "<b>Follow us on <a href=\"http://twitter.com/NASA_GoddardPix\" rel=\"nofollow\">Twitter</a></b>",
            "<b>Follow us on <a href=\"http://twitter.com/NASAGoddardPix\">Twitter</a></b>",
            "<b>Join us on <a href=\"https://www.facebook.com/pages/Greenbelt-MD/NASA-Goddard/395013845897?ref=tsd\">Facebook]</a></b>",
            "<b>Like us on <a href=\"http://www.facebook.com/pages/Greenbelt-MD/NASA-Goddard/395013845897?ref=tsd\">Facebook</a></b>",
            "<b>Like us on <a href=\"http://www.facebook.com/pages/Greenbelt-MD/NASA-Goddard/395013845897?ref=tsd\" rel=\"nofollow\">Facebook</a></b>"
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
    public String getUiRepoId(String repoId) {
        // Remove "nasa" from ids displayed in UI to make the long list fit in screen
        return super.getUiRepoId(repoId).replaceAll("nasa[_-]?", "").replaceAll("_?photos?", "").replace("mission", "")
                .replace("telescope", "").replace("-science", "").replace("-mars", "").replace("-infrared-sounder", "")
                .replace("researchstation", "").replace("lander", "");
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
    protected String getNonFreeLicenceTemplate(FlickrMedia media) {
        return "PD-USGov-NASA";
    }

    @Override
    protected Collection<String> getStringsToRemove(FlickrMedia media) {
        return STRINGS_TO_REMOVE;
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
                    .fabricationMethod("Q725252") // Satellite imagery
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

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "uahirise-mars":
            return Set.of("@HiRISE");
        case "atmospheric-infrared-sounder", "ghrcdaac", "nasaearthobservatory", "earthrightnow":
            return Set.of("@NASAEarth");
        case "nasawebbtelescope":
            return Set.of("@NASAWebb");
        case "nasaarmstrong", "nasadfrc", "nasafo":
            return Set.of("@NASAArmstrong");
        case "nasaedge":
            return Set.of("@NASA_EDGE");
        case "nasaglenn":
            return Set.of("@NASAglenn");
        case "gsfc", "nasa_goddard":
            return Set.of("@NASAGoddard");
        case "nasahubble":
            return Set.of("@NASAHubble");
        case "nasa_ice":
            return Set.of("@NASA_ICE");
        case "nasa2explore", "nasa_jsc_photo", "nasarobonaut":
            return Set.of("@NASA_Johnson");
        case "nasampcv", "nasaorion":
            return Set.of("@NASA_Orion");
        case "nasa_langley", "larc-science":
            return Set.of("@NASA_Langley");
        case "nasamarshall", "nasamarshallphotos":
            return Set.of("@NASA_Marshall");
        case "nasablueshift":
            return Set.of("@NASAUniverse");
        case "nasacolab":
            return Set.of("@NASACoLab");
        case "nasa-jpl":
            return Set.of("@NASAJPL");
        case "oursolarsystem":
            return Set.of("@NASASolarSystem");
        case "nasakennedy":
            return Set.of("@NASAKennedy");
        case "sdomission":
            return Set.of("@NASASun");
        case "43066628@N07":
            return Set.of("@NASAXrocks");
        case "nasahqphoto":
            return Set.of("@nasahqphoto");
        case "hmpresearchstation":
            return Set.of("@HMP");
        case "morpheuslander":
            return Set.of("@MorpheusLander");
        case "40054892@N06", "nasa_appel", "nasa_larss", "nasacommons", "nasaspaceflightawareness", "eospso",
                "nasa_hsf", "nasadesertrats":
        default:
            return Set.of("@NASA");
        }
    }
}
