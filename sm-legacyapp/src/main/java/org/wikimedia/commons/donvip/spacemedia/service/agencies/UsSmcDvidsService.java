package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;

@Service
public class UsSmcDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsSmcDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${ussmc.dvids.units}") Set<String> dvidsUnits, @Value("${ussmc.dvids.min.year}") int minYear) {
        super(repository, "ussmc.dvids", dvidsUnits, minYear);
    }

    @Override
    @Scheduled(fixedRateString = "${ussmc.dvids.update.rate}", initialDelayString = "${ussmc.dvids.initial.delay}")
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space and Missile Systems Center (DVIDS)";
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
