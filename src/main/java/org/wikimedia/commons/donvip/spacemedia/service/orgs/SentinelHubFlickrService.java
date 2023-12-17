package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class SentinelHubFlickrService extends AbstractOrgFlickrService {

    @Autowired
    public SentinelHubFlickrService(FlickrMediaRepository repository,
            @Value("${sentinel.hub.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "sentinel.hub.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "Sentinel Hub (Flickr)";
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected boolean isSatellitePicture(FlickrMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        return Set.of("@sentinel_hub");
    }
}
