package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Service
public class IndividualsFlickrService extends AbstractAgencyFlickrService<FlickrMedia, Long, LocalDateTime> {

    private static final Map<String, List<String>> STRINGS_TO_REMOVE = Map.of("pierre_markuse", List.of(
            "Follow me on Twitter:",
            "<a href=\"https://twitter.com/Pierre_Markuse\" rel=\"nofollow\">twitter.com/Pierre_Markuse</a>",
            "Do you want to support this collection of satellite images? Any donation, no matter how small, would be appreciated. <a href=\"https://www.paypal.com/paypalme/PierreMarkuse\">PayPal me!</a>",
            "Follow me on <a href=\"https://twitter.com/Pierre_Markuse\">Twitter!</a> and <a href=\"https://mastodon.world/@pierre_markuse\">Mastodon!</a>"));

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
    public void updateMedia() throws IOException, UploadException {
        updateFlickrMedia();
    }

    @Override
    protected Collection<String> getStringsToRemove(String pathAlias) {
        return Optional.ofNullable(STRINGS_TO_REMOVE.get(pathAlias)).orElse(List.of());
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, Metadata metadata, boolean includeHidden) {
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
                    result.remove("Photos by the Perseverance rover");
                    result.add("Photos by the Perseverance rover Mastcams");
                } else if (titleLc.contains("navleft") || titleLc.contains("navright")) {
                    result.remove("Photos by the Perseverance rover");
                    result.add("Photos by the Perseverance rover Navcams");
                } else if (titleLc.contains("supercam")) {
                    result.remove("Photos by the Perseverance rover");
                    result.add("Photos by the Perseverance rover SuperCam");
                } else if (titleLc.contains("watson")) {
                    result.remove("Photos by the Perseverance rover");
                    result.add("Photos by WATSON");
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
            break;
        }
        if (includeHidden) {
            switch (media.getPathAlias()) {
            case "geckzilla":
                result.add("Files from Judy Schmidt Flickr stream");
                break;
            case "kevinmgill":
                result.add("Files from Kevin Gill Flickr stream");
                break;
            case "markmccaughrean":
                result.add("Files from Mark McCaughrean Flickr stream");
                break;
            case "pierre_markuse":
                result.add("Files from Pierre Markuse Flickr stream");
                break;
            }
        }
        return result;
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "geckzilla", "markmccaughrean":
            return Set.of("✨");
        case "kevinmgill":
            return Set.of("🪐");
        case "pierre_markuse":
            return Set.of("🌍");
        default:
            return Set.of("❔");
        }
    }

    @Override
    protected Set<String> getMastodonAccounts(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "geckzilla":
            return Set.of("@spacegeck@astrodon.social");
        case "kevinmgill":
            return Set.of("@kevinmgill@deepspace.social");
        case "markmccaughrean":
            return Set.of("@markmccaughrean@mastodon.social");
        case "pierre_markuse":
            return Set.of("@pierre_markuse@mastodon.world");
        case "harrystrangerphotography", "194849271@N04":
            return Set.of("@spacefromspace@spacey.space");
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
        case "markmccaughrean":
            return Set.of("@markmccaughrean");
        case "pierre_markuse":
            return Set.of("@Pierre_Markuse");
        case "harrystrangerphotography", "194849271@N04":
            return Set.of("@Harry__Stranger");
        default:
            return Set.of();
        }
    }
}
