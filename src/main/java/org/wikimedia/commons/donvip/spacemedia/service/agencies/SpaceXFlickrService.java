package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class SpaceXFlickrService extends AbstractAgencyFlickrService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceXFlickrService.class);

    @Autowired
    public SpaceXFlickrService(FlickrMediaRepository repository,
            @Value("${spacex.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "spacex.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "SpaceX";
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
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

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        return Set.of(Emojis.ROCKET);
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        return Set.of("@SpaceX");
    }
}
