package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class UsSmcFlickrService extends AbstractAgencyFlickrService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    private DvidsMediaRepository<DvidsMedia> dvidsRepository;

    @Autowired
    public UsSmcFlickrService(FlickrMediaRepository repository,
            @Value("${ussmc.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, flickrAccounts);
    }

    @Override
    @Scheduled(fixedRateString = "${ussmc.flickr.update.rate}", initialDelayString = "${ussmc.flickr.initial.delay}")
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space and Missile Systems Center (Flickr)";
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
			result.add("Photographs by the Space and Missile Systems Center");
		}
        return result;
    }
}
