package org.wikimedia.commons.donvip.spacemedia.harvester.nasa.flickr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrApiService;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrHarvesterService;

@SpringBootApplication
@Import({FlickrApiService.class, FlickrHarvesterService.class})
public class NasaFlickrApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(NasaFlickrApplication.class, args);
    }
}
