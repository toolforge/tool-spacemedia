package org.wikimedia.commons.donvip.spacemedia.data.hashes;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@SpringJUnitConfig(HashesTestDataJpa.Config.class)
@TestPropertySource("/application-test.properties")
public abstract class HashesTestDataJpa {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    protected void checkInjectedComponentsAreNotNull() {
        assertNotNull(dataSource);
        assertNotNull(jdbcTemplate);
        assertNotNull(entityManager);
    }

    @Configuration
    public static class Config {

        @Bean(name = "hashesDataSourceProperties")
        @ConfigurationProperties("hashes.datasource")
        public DataSourceProperties dataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean(name = "hashesDataSource")
        @ConfigurationProperties("hashes.datasource.hikari")
        public DataSource dataSource(
                @Qualifier("hashesDataSourceProperties") DataSourceProperties dataSourceProperties) {
            return dataSourceProperties.initializeDataSourceBuilder().build();
        }
    }
}
