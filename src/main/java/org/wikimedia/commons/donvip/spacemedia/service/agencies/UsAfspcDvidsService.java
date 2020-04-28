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
public class UsAfspcDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsAfspcDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usafspc.dvids.units}") Set<String> dvidsUnits) {
        super(repository, dvidsUnits);
    }

    @Override
    @Scheduled(fixedRateString = "${usafspc.dvids.update.rate}", initialDelayString = "${usafspc.dvids.initial.delay}")
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Air Force Space Command (DVIDS)";
    }

    @Override
    public List<String> findTemplates(DvidsMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("PD-USGov-Military-Air Force");
        return result;
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            result.add("Photographs by the United States Air Force Space Command");
        }
        return result;
    }
}
