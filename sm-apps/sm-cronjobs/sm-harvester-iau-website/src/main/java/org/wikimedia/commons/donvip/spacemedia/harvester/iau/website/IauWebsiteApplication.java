package org.wikimedia.commons.donvip.spacemedia.harvester.iau.website;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;

@SpringBootApplication
public class IauWebsiteApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(IauWebsiteApplication.class, args);
    }
}
