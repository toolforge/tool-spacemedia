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
import org.wikimedia.commons.donvip.spacemedia.service.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

import com.github.dozermapper.core.Mapper;

@Service
public class NasaFlickrService extends SpaceAgencyFlickrService {

    @Autowired
    public NasaFlickrService(FlickrMediaRepository repository, ProblemRepository problemrepository,
            MediaService mediaService, FlickrService flickrService,
            Mapper dozerMapper, @Value("${nasa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, problemrepository, mediaService, flickrService, dozerMapper, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${nasa.flickr.update.rate}", initialDelayString = "${initial.delay}")
    public List<FlickrMedia> updateMedia() {
        return updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "Nasa (Flickr)";
    }
}
