package org.wikimedia.commons.donvip.spacemedia.data.hashes;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
	    entityManagerFactoryRef = "hashesEntityManagerFactory",
	    transactionManagerRef = "hashesTransactionManager",
	    basePackageClasses = {HashesDbConfiguration.class})
public class HashesDbConfiguration {

    @Bean(name = "hashesDataSourceProperties")
    @ConfigurationProperties("hashes.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "hashesDataSource")
    @ConfigurationProperties("hashes.datasource.hikari")
    public DataSource dataSource(@Qualifier("hashesDataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "hashesEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
            Environment env, @Qualifier("hashesDataSource") DataSource dataSource) {
        Map<String, Object> hibernateProperties = new HashMap<>();
        hibernateProperties.put("hibernate.physical_naming_strategy", new CamelCaseToUnderscoresNamingStrategy());
        hibernateProperties.put("hibernate.implicit_naming_strategy", new SpringImplicitNamingStrategy());
        hibernateProperties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        return builder.dataSource(dataSource)
                .packages(getClass().getPackage().getName())
                .properties(hibernateProperties)
                .persistenceUnit("hashes").build();
    }

    @Bean(name = "hashesTransactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
