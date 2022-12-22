package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;

@Service
public class UsSscDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsSscDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usssc.dvids.units}") Set<String> dvidsUnits, @Value("${usssc.dvids.min.year}") int minYear) {
        super(repository, "usssc.dvids", dvidsUnits, minYear);
    }

    @Override
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space Systems Command (DVIDS)";
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("Photographs by the Space Systems Command");
        }
        return result;
    }
}
