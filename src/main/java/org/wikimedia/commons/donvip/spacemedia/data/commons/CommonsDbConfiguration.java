package org.wikimedia.commons.donvip.spacemedia.data.commons;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "commonsEntityManagerFactory",
    transactionManagerRef = "commonsTransactionManager",
    basePackageClasses = {CommonsDbConfiguration.class})
public class CommonsDbConfiguration {

    @Bean(name = "commonsDataSourceProperties")
    @ConfigurationProperties("commons.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "commonsDataSource")
    @ConfigurationProperties("commons.datasource.hikari")
    public DataSource dataSource(@Qualifier("commonsDataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "commonsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
            @Qualifier("commonsDataSource") DataSource dataSource) {
        return builder.dataSource(dataSource).packages(getClass().getPackage().getName()).persistenceUnit("commons").build();
    }

    @Bean(name = "commonsTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("commonsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
