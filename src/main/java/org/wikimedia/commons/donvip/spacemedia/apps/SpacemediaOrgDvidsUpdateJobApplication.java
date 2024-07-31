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
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.dvids", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, DvidsMedia.class })
public class SpacemediaOrgDvidsUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        app(SpacemediaOrgDvidsUpdateJobApplication.class).run(args);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Org<DvidsMedia> org(@Value("${org}") String org,
            @Value("${dvids.units:*}") Set<String> dvidsUnits,
            @Value("${dvids.countries:*}") Set<String> dvidsCountries,
            @Value("${dvids.min.year}") int minYear,
            @Value("${dvids.blocklist:true}") boolean blocklist,
            @Autowired DvidsMediaRepository<DvidsMedia> repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Org<DvidsMedia>) Class.forName(org)
                .getConstructor(DvidsMediaRepository.class, Set.class, Set.class, int.class, boolean.class)
                .newInstance(repository, dvidsUnits, dvidsCountries, minYear, blocklist);
    }
}
