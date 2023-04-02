package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.service.GoogleTranslateService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.service.TwitterService;

@Configuration
@Import(SpacemediaCommonConfiguration.class)
@ComponentScan(basePackages = { "org.wikimedia.commons.donvip.spacemedia.data",
        "org.wikimedia.commons.donvip.spacemedia.service.wikimedia" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
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
    public GoogleTranslateService translateService() {
        return new GoogleTranslateService();
    }

    @Bean
    public TwitterService twitterService(
            @Value("${twitter.api.oauth1.consumer-token}") String consumerToken,
            @Value("${twitter.api.oauth1.consumer-secret}") String consumerSecret,
            @Value("${twitter.api.oauth1.access-token}") String accessToken,
            @Value("${twitter.api.oauth1.access-secret}") String accessSecret) {
        return new TwitterService(consumerToken, consumerSecret, accessToken, accessSecret);
    }

    @Bean
    public TransactionService transactionService() {
        return new TransactionService();
    }
}
