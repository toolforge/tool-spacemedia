package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class PotentialFlickrService extends AbstractAgencyFlickrService<FlickrMedia, Long, LocalDateTime> {

    @Autowired
    public PotentialFlickrService(FlickrMediaRepository repository,
            @Value("${potential.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "potential.flickr", flickrAccounts);
    }

    @Override
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "Potential Flickr accounts";
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        return Set.of("❔");
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        return Set.of();
    }
}
