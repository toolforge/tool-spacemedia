package org.wikimedia.commons.donvip.spacemedia.harvester.dlr.flickr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrApiService;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrHarvesterService;

@SpringBootApplication
@Import({FlickrApiService.class, FlickrHarvesterService.class})
public class DlrFlickrApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DlrFlickrApplication.class, args);
    }
}
