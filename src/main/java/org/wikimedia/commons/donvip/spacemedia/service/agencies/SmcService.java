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
public class SmcService extends AbstractSpaceAgencyFlickrService {

    @Autowired
    public SmcService(FlickrMediaRepository repository,
			@Value("${smc.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
	@Scheduled(fixedRateString = "${smc.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
		return "Space and Missile Systems Center";
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
	public Set<String> findCategories(FlickrMedia media) {
        Set<String> result = super.findCategories(media);
		result.add("Photographs by the Space and Missile Systems Center");
        return result;
    }
}
