package org.wikimedia.commons.donvip.spacemedia.apps;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@EnableCaching
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(SpacemediaUpdateJobConfiguration.class)
abstract class AbstractSpacemediaOrgUpdateJobApplication implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpacemediaOrgUpdateJobApplication.class);

    @Autowired
    private List<Org<?>> orgs;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            List<String> activeProfiles = Arrays
                    .asList(event.getApplicationContext().getEnvironment().getActiveProfiles());
            if (!activeProfiles.contains("test")) {
                for (Org<?> org : orgs) {
                    if (org.updateOnProfiles(activeProfiles)) {
                        org.updateMedia(event.getArgs());
                    } else {
                        LOGGER.info("{} does not perform media update with profiles {}", org, activeProfiles);
                    }
                }
            }
        } catch (IOException | UploadException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
