package org.wikimedia.commons.donvip.spacemedia.downloader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.data.DomainDbConfiguration;

@SpringBootApplication
@Import(DomainDbConfiguration.class)
public class DownloaderApplication implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DownloaderService service;

    public static void main(String[] args) {
        SpringApplication.run(DownloaderApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        service.downloadFiles();
    }
}
