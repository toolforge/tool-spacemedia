package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class EsaFlickrService extends AbstractSpaceAgencyFlickrService {

    @Autowired
    private EsaMediaRepository esaRepository;

    @Autowired
    public EsaFlickrService(FlickrMediaRepository repository,
            @Value("${esa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${esa.flickr.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "ESA (Flickr)";
    }

    @Override
    protected MediaRepository<?, ?, ?> getOriginalRepository() {
        return esaRepository;
    }
}
