package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.CategorizationService;

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
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia Sentinel Hub Flickr files uploaded by " + commonsService.getAccount();
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
    protected Collection<String> getStringsToRemove(FlickrMedia media) {
        return List.of("Follow us on <a href=\"https://twitter.com/sentinel_hub\">Twitter!</a>");
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("Files from Sentinel Hub Flickr stream");
        }
        categorizationService.findCategoriesForSentinels(media, result);
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        if (categorizationService.isFromSentinelSatellite(media)) {
            result.add(CategorizationService.getCopernicusTemplate(media.getYear().getValue()));
        }
        return result;
    }
}
