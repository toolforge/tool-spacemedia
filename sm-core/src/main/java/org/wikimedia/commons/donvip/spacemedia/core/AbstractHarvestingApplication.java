package org.wikimedia.commons.donvip.spacemedia.core;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.data.DomainDbConfiguration;

@Import(DomainDbConfiguration.class)
public abstract class AbstractHarvestingApplication implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHarvestingApplication.class);

    @Autowired
    private Harvester harvester;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            harvester.harvestMedia();
        } catch (IOException e) {
            LOGGER.error("Error while harvesting media", e);
        }
    }
}
