package org.wikimedia.commons.donvip.spacemedia.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.StatsService;

@RestController
public class StatsController {

    @Autowired
    private StatsService service;

    @GetMapping("/stats")
    public List<Statistics> stats(@RequestParam(defaultValue = "false") boolean details) {
        return service.getStats(details);
    }
}
