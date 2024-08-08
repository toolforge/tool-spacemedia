package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;

@Service
public class UsMilFranceDvidsService extends AbstractOrgDvidsService {

    @Autowired
    public UsMilFranceDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usmilfrance.dvids.units:*}") Set<String> dvidsUnits,
            @Value("${usmilfrance.dvids.countries}") Set<String> dvidsCountries,
            @Value("${usmilfrance.dvids.min.year}") int minYear,
            @Value("${usmilfrance.dvids.blocklist}") boolean blocklist) {
        super(repository, "usmilfrance.dvids", dvidsUnits, dvidsCountries, minYear, blocklist);
    }

    @Override
    public String getName() {
        return "U.S. Military in France (DVIDS)";
    }

    @Override
    protected List<String> getReviewCategories() {
        return List.of("Milimedia files (review needed)");
    }
}
