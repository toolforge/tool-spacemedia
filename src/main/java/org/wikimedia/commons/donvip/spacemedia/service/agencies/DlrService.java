package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class DlrService extends AbstractSpaceAgencyFlickrService {

    @Autowired
    public DlrService(FlickrMediaRepository repository, @Value("${dlr.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${dlr.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            result.add("Files from Deutsches Zentrum für Luft- und Raumfahrt Flickr stream");
        }
        EsaService.enrichEsaCategories(result, media);
        return result;
    }

    @Override
    public String getName() {
        return "DLR";
    }

    @Override
    protected String getLanguage(FlickrMedia media) {
        return media.getDescription().contains("Photo Credit:") ? "en" : "de";
    }
}
