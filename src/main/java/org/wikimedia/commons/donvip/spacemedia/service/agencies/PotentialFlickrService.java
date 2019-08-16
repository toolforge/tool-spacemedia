package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

import com.github.dozermapper.core.Mapper;

@Service
public class PotentialFlickrService extends SpaceAgencyFlickrService {

    @Autowired
    public PotentialFlickrService(FlickrMediaRepository repository, ProblemRepository problemrepository,
            MediaService mediaService, CommonsService commonsService, FlickrService flickrService, Mapper dozerMapper,
            @Value("${potential.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, problemrepository, mediaService, commonsService, flickrService, dozerMapper, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${potential.update.rate}", initialDelayString = "${initial.delay}")
    public List<FlickrMedia> updateMedia() {
        return updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "Potential Flickr accounts";
    }
}
