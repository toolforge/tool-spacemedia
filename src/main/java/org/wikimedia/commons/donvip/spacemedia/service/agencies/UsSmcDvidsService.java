package org.wikimedia.commons.donvip.spacemedia.service.agencies;

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
}
