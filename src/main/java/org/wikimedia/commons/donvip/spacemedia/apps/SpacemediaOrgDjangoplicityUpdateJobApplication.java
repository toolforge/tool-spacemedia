package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, DjangoplicityMedia.class })
public class SpacemediaOrgDjangoplicityUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaOrgDjangoplicityUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Org<?, ?> org(@Value("${org}") String org, @Value("${search.link}") String searchLink,
            @Autowired DjangoplicityMediaRepository repository, ApplicationContext context)
            throws ReflectiveOperationException {
        return (Org<?, ?>) Class.forName(org).getConstructor(DjangoplicityMediaRepository.class, String.class)
                .newInstance(repository, searchLink);
    }
}
