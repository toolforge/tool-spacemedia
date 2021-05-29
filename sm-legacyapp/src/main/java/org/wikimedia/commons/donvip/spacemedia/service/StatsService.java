package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;

@Service
public class StatsService {

    @Autowired
    private List<AbstractAgencyService<?, ?, ?, ?, ?, ?>> agencies;

    public List<Statistics> getStats(boolean details) {
        return agencies.parallelStream().map(a -> a.getStatistics(details)).sorted()
                .collect(Collectors.toList());
    }
}
