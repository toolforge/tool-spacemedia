package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.LocalDateTime;
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
    public Set<String> findCategories(FlickrMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (result.contains("Photos by Martian rover Mastcams") && result.contains("Photos by the Curiosity rover")) {
            result.remove("Photos by Martian rover Mastcams");
            result.remove("Photos by the Curiosity rover");
            result.add("Photos by the Curiosity rover Mastcam");
        } else if (result.contains("Photos by the Perseverance rover") && media.getTitle().contains("Mastcam-Z")) {
            result.remove("Photos by the Perseverance rover");
            result.add("Photos by the Perseverance rover Mastcams");
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
        default:
            return Set.of();
        }
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "geckzilla":
            return Set.of("SpaceGeck");
        case "kevinmgill":
            return Set.of("kevinmgill");
        case "markmccaughrean":
            return Set.of("markmccaughrean");
        case "pierre_markuse":
            return Set.of("Pierre_Markuse");
        default:
            return Set.of();
        }
    }
}
