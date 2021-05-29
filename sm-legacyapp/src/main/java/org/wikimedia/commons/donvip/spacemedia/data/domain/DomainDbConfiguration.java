package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "domainEntityManagerFactory",
    transactionManagerRef = "domainTransactionManager",
    basePackageClasses = {DomainDbConfiguration.class})
public class DomainDbConfiguration {

    @Primary
    @Bean(name = "domainDataSourceProperties")
    @ConfigurationProperties("domain.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "domainDataSource")
    @ConfigurationProperties("domain.datasource.hikari")
    public DataSource dataSource() {
        return dataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "domainEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, Environment env) {
        Map<String, Object> hibernateProperties = new HashMap<>();
        hibernateProperties.put("hibernate.physical_naming_strategy", new SpringPhysicalNamingStrategy());
        hibernateProperties.put("hibernate.implicit_naming_strategy", new SpringImplicitNamingStrategy());
        hibernateProperties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        return builder.dataSource(dataSource())
                .packages(getClass().getPackage().getName())
                .properties(hibernateProperties)
                .persistenceUnit("domain").build();
    }

    @Primary
    @Bean(name = "domainTransactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
