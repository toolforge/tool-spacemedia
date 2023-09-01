package org.wikimedia.commons.donvip.spacemedia.apps;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.DomainDbConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.nasa", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        DomainDbConfiguration.class })
public class SpacemediaOrgUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaOrgUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Org<?> org(@Value("${org}") String org,
            @Value("${repositoryClass}") String repositoryClass, @Value("${repositoryName:}") String repositoryName,
            @Value("${repoIds:}") Set<String> repoIds, ApplicationContext context) throws ReflectiveOperationException {
        Class<?> orgClass = Class.forName(org);
        Class<?> repoClass = Class.forName(repositoryClass);
        MediaRepository<?> repo = (MediaRepository<?>) (repositoryName.isBlank() ? context.getBean(repoClass)
                : context.getBean(repositoryName, repoClass));
        return (Org<?>) (repoIds.isEmpty() ? orgClass.getConstructor(repoClass).newInstance(repo)
                : orgClass.getConstructor(repoClass, Set.class).newInstance(repo, repoIds));
    }
}
