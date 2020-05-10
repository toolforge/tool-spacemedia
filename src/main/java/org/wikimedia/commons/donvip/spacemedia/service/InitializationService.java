package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;

@Service
public class InitializationService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationService.class);

    @Autowired
    private InitializationService self;

    @Autowired
    private StatsService statsService;

    @Autowired
    private List<AbstractAgencyService<?, ?, ?, ?, ?, ?>> agencies;

    @Value("${reset.duplicates}")
    private boolean resetDuplicates;

    @Value("${reset.ignored}")
    private boolean resetIgnored;

    @Value("${reset.hashes}")
    private boolean resetHashes;

    @Value("${reset.problems}")
    private boolean resetProblems;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (resetProblems) {
            LOGGER.info("Reset a total number of {} problems", self.resetProblems());
        }
        if (resetDuplicates) {
            LOGGER.info("Reset a total number of {} duplicates", self.resetDuplicates());
        }
        if (resetIgnored) {
            LOGGER.info("Reset a total number of {} ignored media", self.resetIgnored());
        }
        if (resetHashes) {
            LOGGER.info("Reset a total number of {} perceptual hashes", self.resetHashes());
        }
        // Perform the most exhaustive data-fetching operation to populate all caches at startup
        statsService.getStats();
    }

    @Transactional
    public long resetDuplicates() {
        return agencies.stream().mapToLong(AbstractAgencyService::resetDuplicates).sum();
    }

    @Transactional
    public int resetIgnored() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetIgnored).sum();
    }

    @Transactional
    public int resetHashes() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetPerceptualHashes).sum();
    }

    @Transactional
    public int resetProblems() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetProblems).sum();
    }
}
