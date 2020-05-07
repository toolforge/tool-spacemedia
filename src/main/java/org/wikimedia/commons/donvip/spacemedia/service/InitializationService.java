package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;

@Service
public class InitializationService implements ApplicationRunner {

    @Autowired
    private InitializationService self;

    @Autowired
    private StatsService statsService;

    @Autowired
    private List<AbstractAgencyService<?, ?, ?, ?, ?, ?>> agencies;

    @Value("${reset.hashes}")
    private boolean resetHashes;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (resetHashes) {
            self.resetHashes();
        }
        // Perform the most exhaustive data-fetching operation to populate all caches at startup
        statsService.getStats();
    }

    @Transactional
    public void resetHashes() {
        agencies.forEach(AbstractAgencyService::resetPerceptualHashes);
    }
}
