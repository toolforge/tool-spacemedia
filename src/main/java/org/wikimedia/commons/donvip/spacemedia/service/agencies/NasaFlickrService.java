package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;

@Service
public class NasaFlickrService extends AbstractSpaceAgencyFlickrService {

    @Autowired
    private NasaMediaRepository<NasaMedia> nasaMediaRepository;

    @Autowired
    public NasaFlickrService(FlickrMediaRepository repository,
            @Value("${nasa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${nasa.flickr.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        waitIndexationInitialization();
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "NASA (Flickr)";
    }

    @Override
    protected MediaRepository<?, ?, ?> getOriginalRepository() {
        return nasaMediaRepository;
    }
}
