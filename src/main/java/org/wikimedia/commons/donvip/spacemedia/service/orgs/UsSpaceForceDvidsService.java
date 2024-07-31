package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;

@Service
public class UsSpaceForceDvidsService extends AbstractOrgDvidsService {

    @Autowired
    public UsSpaceForceDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usspaceforce.dvids.units}") Set<String> dvidsUnits,
            @Value("${usspaceforce.dvids.countries:*}") Set<String> dvidsCountries,
            @Value("${usspaceforce.dvids.min.year}") int minYear,
            @Value("${usspaceforce.dvids.blocklist:true}") boolean blocklist) {
        super(repository, "usspaceforce.dvids", dvidsUnits, dvidsCountries, minYear, blocklist);
    }

    @Override
    public String getName() {
        return "U.S. Space Force/Command (DVIDS)";
    }
}
