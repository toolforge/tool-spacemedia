package org.wikimedia.commons.donvip.spacemedia.harvester.eso.website;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;
import org.wikimedia.commons.donvip.spacemedia.repo.eso.EsoHarvesterService;

@SpringBootApplication
@Import(EsoHarvesterService.class)
public class EsoWebsiteApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsoWebsiteApplication.class, args);
    }
}
