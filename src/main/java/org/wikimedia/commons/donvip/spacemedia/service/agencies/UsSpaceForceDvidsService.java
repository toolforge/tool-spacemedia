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
public class UsSpaceForceDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsSpaceForceDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usspaceforce.dvids.units}") Set<String> dvidsUnits,
            @Value("${usspaceforce.dvids.min.year}") int minYear) {
        super(repository, "usspaceforce.dvids", dvidsUnits, minYear);
    }

    public final void checkDvidsCategories() {
        checkCommonsCategories(KEYWORDS_CATS);
    }

    @Override
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space Force (DVIDS)";
    }
}
