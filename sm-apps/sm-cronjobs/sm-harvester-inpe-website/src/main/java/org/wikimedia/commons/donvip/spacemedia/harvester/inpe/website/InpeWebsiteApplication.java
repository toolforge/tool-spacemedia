package org.wikimedia.commons.donvip.spacemedia.harvester.inpe.website;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;

@SpringBootApplication
public class InpeWebsiteApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(InpeWebsiteApplication.class, args);
    }
}
