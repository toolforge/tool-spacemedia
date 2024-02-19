package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrLicense;
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
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden && "noaasatellites".equals(media.getPathAlias())) {
            result.add("Files from NOAA Satellites Flickr stream");
        }
        return result;
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
        result.remove(FlickrLicense.Public_Domain_Mark.getWikiTemplate());
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
