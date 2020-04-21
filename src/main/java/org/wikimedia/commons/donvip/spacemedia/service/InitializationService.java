package org.wikimedia.commons.donvip.spacemedia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class InitializationService implements ApplicationRunner {

    @Autowired
    private StatsService statsService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Perform the most exhaustive data-fetching operation to populate all caches at startup
        statsService.getStats();
    }
}
