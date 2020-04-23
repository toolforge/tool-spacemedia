package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;

@Service
public class UsAfspcDvidsService extends AbstractAgencyDvidsService {

    @Autowired
    public UsAfspcDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usafspc.dvids.units}") Set<String> dvidsUnits) {
        super(repository, dvidsUnits);
    }

    @Override
    @Scheduled(fixedRateString = "${usafspc.dvids.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Air Force Space Command (DVIDS)";
    }
}
