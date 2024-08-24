package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
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

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "U.S. Diplomatic missions Flickr files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected List<String> getReviewCategories(FlickrMedia media) {
        return getDiplomaticReviewCategories(media);
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            UnitedStates.getUsEmbassyCategory(media).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.remove("Flickr-public domain mark");
        result.add("PD-USGov-DOS");
        return result;
    }

    @Override
    protected SdcStatements getStatements(FlickrMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        UnitedStates.getUsEmbassyCreator(media).ifPresent(result::creator);
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
    protected String getLanguage(FlickrMedia media) {
        return UnitedStates.getUsEmbassyLanguage(media, super::getLanguage);
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.FLAG_USA);
        return result;
    }
}
