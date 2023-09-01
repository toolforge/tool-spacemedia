package org.wikimedia.commons.donvip.spacemedia.apps;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.youtube", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, YouTubeVideo.class })
public class SpacemediaOrgYoutubeUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaOrgYoutubeUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Org<?> org(@Value("${org}") String org,
            @Value("${youtube.channels}") Set<String> youtubeChannels,
            @Autowired YouTubeVideoRepository repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Org<?>) Class.forName(org).getConstructor(YouTubeVideoRepository.class, Set.class)
                .newInstance(repository, youtubeChannels);
    }
}
