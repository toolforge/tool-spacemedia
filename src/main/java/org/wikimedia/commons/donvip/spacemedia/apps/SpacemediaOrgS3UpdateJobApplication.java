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
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

import com.amazonaws.regions.Regions;

@ComponentScan(basePackages = "org.wikimedia.commons.donvip.spacemedia.service.s3", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        Media.class, S3Media.class })
public class SpacemediaOrgS3UpdateJobApplication extends AbstractSpacemediaOrgUpdateJobApplication {

    public static void main(String[] args) {
        app(SpacemediaOrgS3UpdateJobApplication.class).run(args);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Org<S3Media> org(@Value("${org}") String org, @Value("${s3.region}") Regions region,
            @Value("${s3.buckets}") Set<String> buckets, @Autowired S3MediaRepository repository,
            ApplicationContext context) throws ReflectiveOperationException {
        return (Org<S3Media>) Class.forName(org).getConstructor(S3MediaRepository.class, Regions.class, Set.class)
                .newInstance(repository, region, buckets);
    }
}
