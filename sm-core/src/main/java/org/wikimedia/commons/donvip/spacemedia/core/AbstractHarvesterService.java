package org.wikimedia.commons.donvip.spacemedia.core;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.DepotRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.FilePublicationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.MediaPublicationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.repository.MetadataRepository;

public abstract class AbstractHarvesterService implements Harvester {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHarvesterService.class);

    @Autowired
    protected MediaPublicationRepository mediaPublicationRepository;

    @Autowired
    protected FilePublicationRepository filePublicationRepository;

    @Autowired
    protected MetadataRepository metadataRepository;

    @Autowired
    protected DepotRepository depotRepository;

    protected final LocalDateTime startUpdateMedia(String name) {
        LOGGER.info("Starting {} media update...", name);
        // RuntimeData runtimeData = getRuntimeData();
        // runtimeData.setLastUpdateStart(LocalDateTime.now());
        // return runtimeDataRepository.save(runtimeData).getLastUpdateStart();
        return LocalDateTime.now();
    }

    protected final void endUpdateMedia(String name, int count, LocalDateTime start) {
        // RuntimeData runtimeData = getRuntimeData();
        LocalDateTime end = LocalDateTime.now();
        // runtimeData.setLastUpdateEnd(end);
        // runtimeData.setLastUpdateDuration(Duration.between(start, end));
        LOGGER.info("{} media update completed: {} media in {}", name, count,
                /* runtimeDataRepository.save(runtimeData).getLastUpdateDuration() */Duration.between(start, end));
    }

    protected static final Set<String> set(String label) {
        return new TreeSet<>(Arrays.asList(label.replace(", ", ",").split(",")));
    }

    protected void addMetadata(MediaPublication pub, String key, Set<String> values) {
        values.forEach(v -> addMetadata(pub, key, v));
    }

    protected void addMetadata(MediaPublication pub, String key, String value) {
        Metadata metadata = new Metadata(key, value);
        Optional<Metadata> opt = metadataRepository.findById(metadata);
        pub.addMetadata(opt.isPresent() ? opt.get() : metadataRepository.save(metadata));
    }

    protected final void problem(URL problematicUrl, String errorMessage) {
        // TODO persist
        LOGGER.error("{}: {}", problematicUrl, errorMessage);
    }
}
