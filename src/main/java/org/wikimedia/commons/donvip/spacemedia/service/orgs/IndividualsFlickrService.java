package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class IndividualsFlickrService extends AbstractOrgFlickrService {

    private static final List<String> STRINGS_TO_REMOVE = List.of(
            "Follow me on Twitter:",
            "<a href=\"https://twitter.com/Pierre_Markuse\" rel=\"nofollow\">twitter.com/Pierre_Markuse</a>",
            "Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href=\"https://www.paypal.com/paypalme/PierreMarkuse\">PayPal me!</a>",
            "Follow me on <a href=\"https://twitter.com/Pierre_Markuse\">Twitter!</a> and <a href=\"https://mastodon.world/@pierre_markuse\">Mastodon!</a>",
            "Feel free to share, giving the appropriate credit and providing a link to the original image or tweet: <a href=\"https://creativecommons.org/licenses/by/3.0/\">creativecommons.org/licenses/by/3.0/</a>",
            "Feel free to share, giving the appropriate credit and providing a link to the original image or tweet <a href=\"https://creativecommons.org/licenses/by/3.0/\">creativecommons.org/licenses/by/3.0/</a>");

    @Autowired
    public IndividualsFlickrService(FlickrMediaRepository repository,
            @Value("${individuals.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "individuals", flickrAccounts);
    }

    @Override
    public String getName() {
        return "Individuals (Flickr)";
    }

    @Override
    protected Collection<String> getStringsToRemove(FlickrMedia media) {
        return STRINGS_TO_REMOVE;
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        switch (media.getPathAlias()) {
        case "harrystrangerphotography", "194849271@N04":
            if ((media.getDescription() != null && media.getDescription().toLowerCase(Locale.ENGLISH).contains("usgs"))
                    || media.getPhotosets().stream().anyMatch(s -> s.getTitle().startsWith("KH-"))) {
                result.add("PD-USGov-NRO");
                replace(result, "Flickr-public domain mark", "PD-USGov-USGS");
            }
            break;
        }
        return result;
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        String titleLc = media.getTitle().toLowerCase(Locale.ENGLISH);
        switch (media.getPathAlias()) {
        case "kevinmgill":
            if (result.contains("Photos by the Curiosity rover")
                    && (result.contains("Photos by Martian rover Mastcams") || titleLc.contains("mastcam"))) {
                result.remove("Photos by Martian rover Mastcams");
                result.remove("Photos by the Curiosity rover");
                result.add("Photos by the Curiosity rover Mastcam");
            } else if (result.contains("Photos by the Perseverance rover")) {
                if (titleLc.contains("mastcam")) {
                    replace(result, "Photos by the Perseverance rover", "Photos by the Perseverance rover Mastcams");
                } else if (titleLc.contains("navleft") || titleLc.contains("navright")) {
                    replace(result, "Photos by the Perseverance rover", "Photos by the Perseverance rover Navcams");
                } else if (titleLc.contains("supercam")) {
                    replace(result, "Photos by the Perseverance rover", "Photos by the Perseverance rover SuperCam");
                } else if (titleLc.contains("watson")) {
                    replace(result, "Photos by the Perseverance rover", "Photos by WATSON");
                }
            }
            break;
        case "pierre_markuse":
            if (result.contains("Satellite pictures of tropical cyclones") && result.contains("Photos by VIIRS")) {
                result.remove("Satellite pictures of tropical cyclones");
                result.remove("Photos by Suomi NPP");
                result.remove("Photos by VIIRS");
                result.add("Photos of tropical cyclones by VIIRS");
            } else if (media.getTags().contains("sentinel") && media.getTags().contains("fire")) {
                result.add("Photos of wildfires by Sentinel satellites");
            } else if (media.getTags().contains("sentinel") && media.getTags().contains("flood")) {
                result.add("Photos of floods by Sentinel satellites");
            }
            for (String sentinel : new String[] { "Sentinel-1", "Sentinel-2", "Sentinel-3" }) {
                if (result.contains(sentinel + " images")) {
                    result.addAll(findCategoriesForEarthObservationImage(media,
                            x -> "Photos of " + x + " by " + sentinel, sentinel + " images"));
                }
            }
            break;
        }
        if (result.contains("Photos of Jupiter system by spacecraft") && result.contains("Photos by JunoCam")) {
            replace(result, "Photos of Jupiter system by spacecraft", "Photos of Jupiter system by JunoCam");
        }
        if (result.contains("Jupiter (planet)") && result.contains("Photos by JunoCam")) {
            replace(result, "Jupiter (planet)", "Photos of Jupiter by JunoCam");
        }
        if (includeHidden) {
            switch (media.getPathAlias()) {
            case "geckzilla":
                result.add("Files from Judy Schmidt Flickr stream");
                break;
            case "kevinmgill":
                result.add("Files from Kevin Gill Flickr stream");
                break;
            case "pierre_markuse":
                result.add("Files from Pierre Markuse Flickr stream");
                break;
            case "192271236@N03":
                result.add("Files from Andrea Luck Flickr stream");
                break;
            }
        }
        return result;
    }

    @Override
    protected boolean isSatellitePicture(FlickrMedia media, FileMetadata metadata) {
        return Set.of("pierre_markuse", "harrystrangerphotography", "194849271@N04").contains(media.getPathAlias());
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        switch (uploadedMedia.getPathAlias()) {
        case "geckzilla":
            result.add(Emojis.STARS);
            break;
        case "kevinmgill", "192271236@N03":
            result.add(Emojis.PLANET_WITH_RINGS);
            break;
        case "pierre_markuse", "harrystrangerphotography", "194849271@N04":
            result.add(Emojis.EARTH_EUROPE);
            result.add(Emojis.SATELLITE);
            break;
        default:
            result.add("❔");
        }
        return result;
    }

    @Override
    protected Set<String> getMastodonAccounts(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "geckzilla":
            return Set.of("@spacegeck@astrodon.social");
        case "kevinmgill":
            return Set.of("@kevinmgill@deepspace.social");
        case "pierre_markuse":
            return Set.of("@pierre_markuse@mastodon.world");
        case "harrystrangerphotography", "194849271@N04":
            return Set.of("@spacefromspace@spacey.space");
        case "192271236@N03":
            return Set.of("@andrealuck@fosstodon.org");
        default:
            return Set.of();
        }
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "geckzilla":
            return Set.of("@SpaceGeck");
        case "kevinmgill":
            return Set.of("@kevinmgill");
        case "pierre_markuse":
            return Set.of("@Pierre_Markuse");
        case "harrystrangerphotography", "194849271@N04":
            return Set.of("@Harry__Stranger");
        case "192271236@N03":
            return Set.of("@andrluck");
        default:
            return Set.of();
        }
    }
}
