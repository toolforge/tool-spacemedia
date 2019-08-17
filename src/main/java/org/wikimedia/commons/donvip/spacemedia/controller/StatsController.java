package org.wikimedia.commons.donvip.spacemedia.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;

@RestController()
@RequestMapping
public class StatsController {

    @Autowired
    private List<AbstractSpaceAgencyService<?, ?>> agencies;

    @GetMapping("/stats")
    public List<Statistics> stats() {
        return agencies.parallelStream()
                .map(AbstractSpaceAgencyService::getStatistics)
                .sorted()
                .collect(Collectors.toList());
    }
}
