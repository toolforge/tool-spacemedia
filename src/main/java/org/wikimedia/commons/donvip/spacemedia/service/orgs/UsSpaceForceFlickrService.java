package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

@Service
public class UsSpaceForceFlickrService extends AbstractOrgFlickrService {

    @Autowired
    public UsSpaceForceFlickrService(FlickrMediaRepository repository,
            @Value("${usspaceforce.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "usspaceforce.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "U.S. Space Force/Command (Flickr)";
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        if (FlickrLicense.of(media.getLicense()) == FlickrLicense.United_States_Government_Work
                || (media.getDescription() != null && media.getDescription().contains("Air Force photo"))) {
            result.remove(FlickrLicense.United_States_Government_Work.getWikiTemplate());
            result.add("PD-USGov-Military-Air Force");
        }
        return result;
    }

    @Override
    protected List<String> getReviewCategories() {
        List<String> result = new ArrayList<>(super.getReviewCategories());
        result.add("Milimedia files (review needed)");
        return result;
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(UnitedStates.getUsMilitaryEmoji(uploadedMedia));
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(UnitedStates.getUsMilitaryTwitterAccount(uploadedMedia));
        return result;
    }
}
