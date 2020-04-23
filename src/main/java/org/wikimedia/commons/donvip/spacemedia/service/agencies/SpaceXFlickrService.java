package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class SpaceXFlickrService extends AbstractAgencyFlickrService {

    @Autowired
    public SpaceXFlickrService(FlickrMediaRepository repository,
            @Value("${spacex.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${spacex.flickr.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "SpaceX";
    }

    @Override
    public List<String> findTemplates(FlickrMedia media) {
        List<String> result = super.findTemplates(media);
        if (FlickrFreeLicense.of(media.getLicense()) == FlickrFreeLicense.Public_Domain_Dedication_CC0) {
            result.remove(FlickrFreeLicense.Public_Domain_Dedication_CC0.getWikiTemplate());
            result.add("Cc-zero-SpaceX");
        }
        return result;
    }
}
