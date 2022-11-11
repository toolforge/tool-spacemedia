package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;

@Service
public class UsAfspcDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsAfspcDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usafspc.dvids.units}") Set<String> dvidsUnits, @Value("${usafspc.dvids.min.year}") int minYear) {
        super(repository, "usafspc.dvids", dvidsUnits, minYear);
    }

    @Override
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Air Force Space Command (DVIDS)";
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
