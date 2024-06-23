package org.wikimedia.commons.donvip.spacemedia.apps;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.flickr", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, FlickrMedia.class })
public class SpacemediaOrgFlickrUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        app(SpacemediaOrgFlickrUpdateJobApplication.class).run(args);
    }

    @Bean
    public Org<?> org(@Value("${org}") String org,
            @Value("${flickr.accounts}") Set<String> flickrAccounts,
            @Autowired FlickrMediaRepository repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Org<?>) Class.forName(org).getConstructor(FlickrMediaRepository.class, Set.class)
                .newInstance(repository, flickrAccounts);
    }
}
