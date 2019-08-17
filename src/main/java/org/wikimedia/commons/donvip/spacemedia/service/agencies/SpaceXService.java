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
public class SpaceXService extends AbstractSpaceAgencyFlickrService {

    @Autowired
    public SpaceXService(FlickrMediaRepository repository,
            @Value("${spacex.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${spacex.update.rate}", initialDelayString = "${initial.delay}")
    public List<FlickrMedia> updateMedia() {
        return updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "SpaceX";
    }

    @Override
    protected List<String> findTemplates(FlickrMedia media) {
        List<String> result = super.findTemplates(media);
        FlickrFreeLicense license = FlickrFreeLicense.of(media.getLicense());
        if (license == FlickrFreeLicense.Public_Domain_Dedication_CC0) {
            result.add("Cc-zero-SpaceX");
        } else {
            result.add(license.getWikiTemplate());
        }
        return result;
    }
}
