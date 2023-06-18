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
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.box", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, BoxMedia.class })
public class SpacemediaOrgBoxUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaOrgBoxUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Org<?, ?, ?> org(@Value("${org}") String org,
            @Value("${box.app-shares}") Set<String> boxAppShares,
            @Autowired BoxMediaRepository repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Org<?, ?, ?>) Class.forName(org).getConstructor(BoxMediaRepository.class, Set.class)
                .newInstance(repository, boxAppShares);
    }
}
