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
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.flickr", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
public class SpacemediaAgencyFlickrUpdateJobApplication extends AbstractSpacemediaAgencyUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaAgencyFlickrUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Agency<?, ?, ?> agency(@Value("${agency}") String agency,
            @Value("${flickr.accounts}") Set<String> flickrAccounts,
            @Autowired FlickrMediaRepository repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Agency<?, ?, ?>) Class.forName(agency).getConstructor(FlickrMediaRepository.class, Set.class)
                .newInstance(repository, flickrAccounts);
    }
}
