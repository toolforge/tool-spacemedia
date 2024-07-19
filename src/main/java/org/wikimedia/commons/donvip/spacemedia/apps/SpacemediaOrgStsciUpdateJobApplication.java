package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

@ComponentScan(basePackages = { "org.wikimedia.commons.donvip.spacemedia.service.nasa",
        "org.wikimedia.commons.donvip.spacemedia.service.stsci" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, StsciMedia.class, DjangoplicityMedia.class })
public class SpacemediaOrgStsciUpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        app(SpacemediaOrgStsciUpdateJobApplication.class).run(args);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Org<DjangoplicityMedia> django(@Value("${django}") String org,
            @Value("${django.search.link}") String searchLink,
            @Autowired DjangoplicityMediaRepository repository, ApplicationContext context)
            throws ReflectiveOperationException {
        return (Org<DjangoplicityMedia>) Class.forName(org)
                .getConstructor(DjangoplicityMediaRepository.class, String.class)
                .newInstance(repository, searchLink);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Org<StsciMedia> stsci(@Value("${stsci}") String org, @Value("${stsci.search.link}") String searchLink,
            @Value("${stsci.detail.link}") String detailLink, @Autowired StsciMediaRepository repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Org<StsciMedia>) Class.forName(org)
                .getConstructor(StsciMediaRepository.class, String.class, String.class)
                .newInstance(repository, searchLink, detailLink);
    }
}
