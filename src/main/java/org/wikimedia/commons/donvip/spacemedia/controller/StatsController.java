package org.wikimedia.commons.donvip.spacemedia.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.SpaceAgencyService;

@RestController()
@RequestMapping
public class StatsController {

    @Autowired
    private List<SpaceAgencyService<?, ?>> agencies;

    @GetMapping("/stats")
    public List<Statistics> stats() {
        return agencies.parallelStream()
                .map(SpaceAgencyService::getStatistics)
                .sorted()
                .collect(Collectors.toList());
    }
}
