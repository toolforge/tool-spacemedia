package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Service
public class IndividualsFlickrService extends AbstractAgencyFlickrService<FlickrMedia, Long, LocalDateTime> {

    protected IndividualsFlickrService(FlickrMediaRepository repository,
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
