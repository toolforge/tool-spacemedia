package org.wikimedia.commons.donvip.spacemedia.core;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.data.DomainDbConfiguration;

@Import(DomainDbConfiguration.class)
public abstract class AbstractHarvestingApplication {

    @Autowired
    private Harvester harvester;

    @PostConstruct
    public void init() throws IOException {
        harvester.harvestMedia();
    }
}
