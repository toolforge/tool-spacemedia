package org.wikimedia.commons.donvip.spacemedia.harvester.esa.hubble;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;
import org.wikimedia.commons.donvip.spacemedia.repo.eso.EsoHarvesterService;

@SpringBootApplication
@Import(EsoHarvesterService.class)
public class EsaHubbleApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsaHubbleApplication.class, args);
    }
}
