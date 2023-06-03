package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.wikimedia.commons.donvip.spacemedia.data.domain.DomainDbConfiguration;

import com.twelvemonkeys.servlet.image.IIOProviderContextListener;

@EnableAsync
@EnableCaching
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = SolrAutoConfiguration.class)
@Import(SpacemediaCommonConfiguration.class)
@ComponentScan(basePackages = { "org.wikimedia.commons.donvip.spacemedia.controller",
        "org.wikimedia.commons.donvip.spacemedia.data",
        "org.wikimedia.commons.donvip.spacemedia.service" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        DomainDbConfiguration.class })
public class SpacemediaWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpacemediaWebApplication.class, args);
    }

    @Bean
    public IIOProviderContextListener iioProviderContextListener() {
        return new IIOProviderContextListener();
    }
}
