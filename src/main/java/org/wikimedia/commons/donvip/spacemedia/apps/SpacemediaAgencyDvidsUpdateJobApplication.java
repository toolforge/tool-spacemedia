package org.wikimedia.commons.donvip.spacemedia.apps;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;

@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, DvidsMedia.class })
public class SpacemediaAgencyDvidsUpdateJobApplication extends AbstractSpacemediaAgencyUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaAgencyDvidsUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Agency<?, ?, ?> agency(@Value("${agency}") String agency,
            @Value("${dvids.units}") Set<String> dvidsUnits, @Value("${dvids.min.year}") int minYear,
            @Autowired DvidsMediaRepository<DvidsMedia> repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Agency<?, ?, ?>) Class.forName(agency)
                .getConstructor(DvidsMediaRepository.class, Set.class, int.class)
                .newInstance(repository, dvidsUnits, minYear);
    }
}
