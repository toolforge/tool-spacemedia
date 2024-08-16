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
public class UsEmbassiesFlickrService extends AbstractOrgFlickrService {

    @Autowired
    public UsEmbassiesFlickrService(FlickrMediaRepository repository,
            @Value("${usembassies.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "usembassies.flickr", flickrAccounts);
    }

    @Override
    public String getName() {
        return "U.S. Embassies (Flickr)";
    }

    protected boolean checkBlocklist() {
        return false;
    }

    protected String hiddenUploadCategory() {
        return "U.S. Embassies Flickr files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected List<String> getReviewCategories() {
        return List.of("Diplomedia files (review needed)");
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-DOS");
        return result;
    }

    @Override
    public String getUiRepoId(String repoId) {
        // Remove "usembassy" from ids displayed in UI to make the long list fit in screen
        return super.getUiRepoId(repoId).replace("-", "").replace("_", "").replace("usembassy", "").replace("usemb", "")
            .replace("embassy", "").replace("usmissionto", "").replace("usmission", "").replace("usin", "")
            .replace("embaixadaeua", "").replace("publicdiplomacy", "").replace("usconsulategeneral", "")
            .replace("usconsulate", "").replace("consulate", "").replace("uscg", "").replace("consulado", "")
            .replace("uscons", "").replace("congen", "");
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
        result.add(UnitedStates.getUsEmbassyTwitterAccount(uploadedMedia));
        return result;
    }
}
