package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrFreeLicense;

@Service
public class SpaceXFlickrService extends AbstractAgencyFlickrService<FlickrMedia, Long, LocalDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceXFlickrService.class);

    @Autowired
    public SpaceXFlickrService(FlickrMediaRepository repository,
            @Value("${spacex.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "spacex.flickr", flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${spacex.flickr.update.rate}", initialDelayString = "${spacex.flickr.initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "SpaceX";
    }

    @Override
    public Set<String> findTemplates(FlickrMedia media) {
        Set<String> result = super.findTemplates(media);
        try {
            if (FlickrFreeLicense.of(media.getLicense()) == FlickrFreeLicense.Public_Domain_Dedication_CC0) {
                result.remove(FlickrFreeLicense.Public_Domain_Dedication_CC0.getWikiTemplate());
                result.add("Cc-zero-SpaceX");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.debug(e.getMessage());
        }
        return result;
    }
}
