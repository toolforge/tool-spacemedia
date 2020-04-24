package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;

@Service
public class UsSmcDvidsService extends AbstractAgencyDvidsService {

    @Autowired
    public UsSmcDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${ussmc.dvids.units}") Set<String> dvidsUnits) {
        super(repository, dvidsUnits);
    }

    @Override
    @Scheduled(fixedRateString = "${ussmc.dvids.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space and Missile Systems Center (DVIDS)";
    }

    @Override
    public List<String> findTemplates(DvidsMedia media) {
        List<String> result = super.findTemplates(media);
        if (media.getDescription().contains("Space Force photo")) {
            result.add("PD-USGov-Military-Space Force");
        } else {
            result.add("PD-USGov-Military-Air Force");
        }
        return result;
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            result.add("Photographs by the Space and Missile Systems Center");
        }
        return result;
    }
}
