package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgService;

@Service
public class StatsService {

    @Autowired
    private List<AbstractOrgService<?>> orgs;

    public List<Statistics> getStats(boolean details) {
        return orgs.parallelStream().map(a -> a.getStatistics(details)).sorted().toList();
    }
}
