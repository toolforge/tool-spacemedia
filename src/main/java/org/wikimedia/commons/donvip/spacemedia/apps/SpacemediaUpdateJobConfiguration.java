package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;

@Configuration
@Import(SpacemediaCommonConfiguration.class)
@ComponentScan(basePackages = { "org.wikimedia.commons.donvip.spacemedia.data",
        "org.wikimedia.commons.donvip.spacemedia.service.wikimedia" })
public class SpacemediaUpdateJobConfiguration {

    @Bean
    public MediaService mediaService() {
        return new MediaService();
    }

    @Bean
    public RemoteService remoteService() {
        return new RemoteService();
    }

    @Bean
    public SearchService searchService() {
        return new SearchService();
    }

    @Bean
    public TransactionService transactionService() {
        return new TransactionService();
    }
}
