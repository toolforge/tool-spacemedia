package org.wikimedia.commons.donvip.spacemedia.apps;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, StacMedia.class })
public class SpacemediaOrgStacUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        app(SpacemediaOrgStacUpdateJobApplication.class).run(args);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Org<StacMedia> org(@Value("${org}") String org, @Value("${stac.catalogs}") Set<String> stacCatalogs,
            @Autowired StacMediaRepository repository, ApplicationContext context) throws ReflectiveOperationException {
        return (Org<StacMedia>) Class.forName(org)
                .getConstructor(StacMediaRepository.class, Set.class).newInstance(repository, stacCatalogs);
    }
}
