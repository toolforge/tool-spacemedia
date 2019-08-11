package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

import com.github.dozermapper.core.Mapper;

@Service
public class SpaceXService extends SpaceAgencyFlickrService {

    @Autowired
    public SpaceXService(FlickrMediaRepository repository, MediaService mediaService, FlickrService flickrService,
            Mapper dozerMapper, @Value("${spacex.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, mediaService, flickrService, dozerMapper, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${spacex.update.rate}")
    public List<FlickrMedia> updateMedia() {
        return updateFlickrMedia("SpaceX");
    }
}
