package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;

@Service
public class UsAirForceDvidsService extends AbstractOrgDvidsService {

    @Autowired
    public UsAirForceDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usairforce.dvids.units}") Set<String> dvidsUnits,
            @Value("${usairforce.dvids.min.year}") int minYear) {
        super(repository, "usairforce.dvids", dvidsUnits, minYear);
    }

    @Override
    public String getName() {
        return "U.S. Air Force (DVIDS)";
    }
}
