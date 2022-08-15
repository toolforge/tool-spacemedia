package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;

@Service
public class InitializationService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationService.class);

    @Autowired
    private InitializationService self;

    @Autowired
    private StatsService statsService;

    @Autowired
    private HashAssociationRepository hashAssociationRepository;

    @Autowired
    private List<AbstractAgencyService<?, ?, ?, ?, ?, ?>> agencies;

    @Value("${reset.duplicates}")
    private boolean resetDuplicates;

    @Value("${reset.ignored}")
    private boolean resetIgnored;

    @Value("${reset.perceptual.hashes}")
    private boolean resetPerceptualHashes;

    @Value("${reset.sha1.hashes}")
    private boolean resetSha1Hashes;

    @Value("${migrate.obsolete.hashes}")
    private boolean migrateObosoleteHashes;

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
        if (resetPerceptualHashes) {
            LOGGER.info("Reset a total number of {} perceptual hashes", self.resetPerceptualHashes());
        }
        if (resetSha1Hashes) {
            LOGGER.info("Reset a total number of {} SHA-1 hashes", self.resetSha1Hashes());
        }
        if (migrateObosoleteHashes) {
            LOGGER.info("Reset a total number of {} SHA-1 hashes", self.migrateObsoleteHashes());
        }
        // Perform the most exhaustive data-fetching operation to populate all caches at startup
        statsService.getStats(true);
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
    public int resetPerceptualHashes() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetPerceptualHashes).sum();
    }

    @Transactional
    public int resetSha1Hashes() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetSha1Hashes).sum();
    }

    @Transactional
    public int resetProblems() {
        return agencies.stream().mapToInt(AbstractAgencyService::resetProblems).sum();
    }

    @Transactional
    public int migrateObsoleteHashes() {
        List<String> obsoleteHashes = hashAssociationRepository.findObsoleteSha1();
        obsoleteHashes.forEach(self::migrateObsoleteHash);
        return obsoleteHashes.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void migrateObsoleteHash(String sha1) {
        String sha1base36 = CommonsService.base36Sha1(sha1);
        if (!hashAssociationRepository.existsById(sha1base36)) {
            hashAssociationRepository.save(new HashAssociation(sha1base36, hashAssociationRepository.findById(sha1)
                    .orElseThrow(() -> new IllegalStateException(sha1)).getPhash()));
        }
        hashAssociationRepository.deleteById(sha1);
    }
}
