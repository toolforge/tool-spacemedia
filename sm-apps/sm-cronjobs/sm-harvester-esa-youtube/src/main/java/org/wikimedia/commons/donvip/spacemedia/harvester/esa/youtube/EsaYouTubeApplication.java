package org.wikimedia.commons.donvip.spacemedia.harvester.esa.youtube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvestingApplication;
import org.wikimedia.commons.donvip.spacemedia.repo.youtube.YouTubeApiService;
import org.wikimedia.commons.donvip.spacemedia.repo.youtube.YouTubeHarvesterService;

@SpringBootApplication
@Import({YouTubeApiService.class, YouTubeHarvesterService.class})
public class EsaYouTubeApplication extends AbstractHarvestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsaYouTubeApplication.class, args);
    }
}
