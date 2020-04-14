package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class AfspcService extends AbstractSpaceAgencyFlickrService {

    @Autowired
    public AfspcService(FlickrMediaRepository repository,
            @Value("${afspc.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${afspc.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "Air Force Space Command";
    }

    @Override
    public List<String> findTemplates(FlickrMedia media) {
        List<String> result = super.findTemplates(media);
        if (FlickrFreeLicense.of(media.getLicense()) == FlickrFreeLicense.United_States_Government_Work
				|| (media.getDescription() != null && media.getDescription().contains("Air Force photo"))) {
            result.remove(FlickrFreeLicense.United_States_Government_Work.getWikiTemplate());
            result.add("PD-USGov-Military-Air Force");
        }
        return result;
    }

    @Override
	public Set<String> findCategories(FlickrMedia media, boolean includeHidden) {
		Set<String> result = super.findCategories(media, includeHidden);
		if (includeHidden) {
			result.add("Photographs by the United States Air Force Space Command");
		}
        return result;
    }
}
