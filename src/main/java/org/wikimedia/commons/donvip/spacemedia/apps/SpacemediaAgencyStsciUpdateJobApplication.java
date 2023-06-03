package org.wikimedia.commons.donvip.spacemedia.apps;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.stsci", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, StsciMedia.class })
public class SpacemediaAgencyStsciUpdateJobApplication extends AbstractSpacemediaAgencyUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaAgencyStsciUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Agency<?, ?, ?> agency(@Value("${agency}") String agency, @Value("${search.link}") String searchLink,
            @Value("${detail.link}") String detailLink, @Autowired StsciMediaRepository repository,
            ApplicationContext context)
            throws ReflectiveOperationException {
        return (Agency<?, ?, ?>) Class.forName(agency)
                .getConstructor(StsciMediaRepository.class, String.class, String.class)
                .newInstance(repository, searchLink, detailLink);
    }
}
