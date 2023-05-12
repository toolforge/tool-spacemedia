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
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;

@Service
public class InitializationService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationService.class);

    @Autowired
    private InitializationService self;

    @Autowired
    private StatsService statsService;

    @Autowired
    private List<AbstractAgencyService<?, ?, ?>> agencies;

    @Autowired
    private FileMetadataRepository metadataRepo;

    @Value("${reset.ignored}")
    private boolean resetIgnored;

    @Value("${reset.perceptual.hashes}")
    private boolean resetPerceptualHashes;

    @Value("${reset.sha1.hashes}")
    private boolean resetSha1Hashes;

    @Value("${reset.problems}")
    private boolean resetProblems;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (resetProblems) {
            LOGGER.info("Reset a total number of {} problems", self.resetProblems());
        }
        if (resetIgnored) {
            LOGGER.info("Reset a total number of {} ignored media", self.resetIgnored());
        }
        if (resetPerceptualHashes) {
            LOGGER.info("Reset a total number of {} perceptual hashes", self.resetPerceptualHashes());
        }
        if (resetSha1Hashes) {
            LOGGER.info("Reset a total number of {} SHA-1 hashes", self.resetSha1Hashes());
        }
        // Perform the most exhaustive data-fetching operation to populate all caches at startup
        statsService.getStats(true);
    }

    @Transactional
    public int resetIgnored() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetIgnored).sum();
    }

    @Transactional
    public int resetPerceptualHashes() {
        return metadataRepo.resetPerceptualHashes();
    }

    @Transactional
    public int resetSha1Hashes() {
        return metadataRepo.resetSha1Hashes();
    }

    @Transactional
    public int resetProblems() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetProblems).sum();
    }
}
