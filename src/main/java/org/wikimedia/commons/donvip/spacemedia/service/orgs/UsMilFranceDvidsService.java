package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;
import java.util.stream.IntStream;

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
    protected int[] getDays(int year, int month) {
        return year >= 1944 && month == 6 ? IntStream.range(1, 31).map(i -> 31 - i).toArray() : super.getDays(year, month);
    }
}
