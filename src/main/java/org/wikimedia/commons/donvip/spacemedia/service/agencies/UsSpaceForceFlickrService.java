package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class UsSpaceForceFlickrService extends AbstractAgencyFlickrService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    private DvidsMediaRepository<DvidsMedia> dvidsRepository;

    @Autowired
    public UsSpaceForceFlickrService(FlickrMediaRepository repository,
            @Value("${usspaceforce.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "usspaceforce.flickr", flickrAccounts);
    }

    @Override
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space Force/Command (Flickr)";
    }

    @Override
    protected DvidsMediaRepository<DvidsMedia> getOriginalRepository() {
        return dvidsRepository;
    }

    @Override
    protected DvidsMediaTypedId getOriginalId(String id) {
        return new DvidsMediaTypedId(id);
    }

    @Override
    public Set<String> findTemplates(FlickrMedia media) {
        Set<String> result = super.findTemplates(media);
        if (FlickrFreeLicense.of(media.getLicense()) == FlickrFreeLicense.United_States_Government_Work
                || (media.getDescription() != null && media.getDescription().contains("Air Force photo"))) {
            result.remove(FlickrFreeLicense.United_States_Government_Work.getWikiTemplate());
            result.add("PD-USGov-Military-Air Force");
        }
        return result;
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            switch (media.getPathAlias()) {
            case "airforcespacecommand":
                result.add("Photographs by the United States Air Force Space Command");
                break;
            case "129133022@N07":
                result.add("Photographs by the Space Systems Command");
                break;
            default:
                // Do nothing
            }
        }
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        if ("129133022@N07".equals(uploadedMedia.getPathAlias())) {
            return Set.of("USSF_SSC");
        } else {
            return Set.of("SpaceForceDoD");
        }
    }
}
