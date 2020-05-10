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
public class UsSpaceCommandDvidsService
        extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsSpaceCommandDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usspacecommand.dvids.units}") Set<String> dvidsUnits,
            @Value("${usspacecommand.dvids.min.year}") int minYear) {
        super(repository, "usspacecommand.dvids", dvidsUnits, minYear);
    }

    @Override
    @Scheduled(fixedRateString = "${usspacecommand.dvids.update.rate}", initialDelayString = "${usspacecommand.dvids.initial.delay}")
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space Command (DVIDS)";
    }
}
