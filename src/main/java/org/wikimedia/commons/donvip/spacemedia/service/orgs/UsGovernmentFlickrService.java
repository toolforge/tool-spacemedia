package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

@Service
public class UsGovernmentFlickrService extends AbstractOrgFlickrService {

    @Autowired
    public UsGovernmentFlickrService(FlickrMediaRepository repository,
            @Value("${usgovernment.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "usgovernment.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "U.S. Government (Flickr)";
    }

    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "U.S. Government Flickr files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected List<String> getReviewCategories(FlickrMedia media) {
        return getGovernmentReviewCategories(media);
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add(UnitedStates.getUsGovernmentLicence(media));
        return result;
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.FLAG_USA);
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(UnitedStates.getUsGovernmentTwitterAccount(uploadedMedia));
        return result;
    }
}
