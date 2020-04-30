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

@Service
public class UsSpaceForceDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsSpaceForceDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usspaceforce.dvids.units}") Set<String> dvidsUnits,
            @Value("${usspaceforce.dvids.min.year}") int minYear) {
        super(repository, dvidsUnits, minYear);
    }

    @Override
    @Scheduled(fixedRateString = "${usspaceforce.dvids.update.rate}", initialDelayString = "${usspaceforce.dvids.initial.delay}")
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space Force (DVIDS)";
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
}
