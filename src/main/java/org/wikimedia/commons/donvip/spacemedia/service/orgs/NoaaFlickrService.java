package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class NoaaFlickrService extends AbstractOrgFlickrService {

    @Autowired
    public NoaaFlickrService(FlickrMediaRepository repository,
            @Value("${noaa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "noaa.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "NOAA (Flickr)";
    }

    @Override
    protected String getNonFreeLicenceTemplate(FlickrMedia media) {
        return "PD-USGov-NOAA";
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add(media.getDescription() != null
                && media.getDescription().toLowerCase(Locale.ENGLISH).contains("credit: nasa/") ? "PD-USGov-NASA"
                        : "PD-USGov-NOAA");
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        switch (uploadedMedia.getPathAlias()) {
        case "noaasatellites":
            return Set.of("@NOAASatellites");
        default:
            return Set.of("@NOAA");
        }
    }
}
