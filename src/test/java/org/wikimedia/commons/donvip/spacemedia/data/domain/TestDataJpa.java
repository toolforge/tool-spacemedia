package org.wikimedia.commons.donvip.spacemedia.data.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class TestDataJpa {
    @Autowired
    protected DataSource dataSource;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected EntityManager entityManager;

    @Test
    void injectedComponentsAreNotNull() {
        assertNotNull(dataSource);
        assertNotNull(jdbcTemplate);
        assertNotNull(entityManager);
    }

    @Configuration
    public static class Config {
        @Bean
        public EmbeddedDatabaseFactoryBean embeddedDatabaseFactory() {
            EmbeddedDatabaseFactoryBean factory = new EmbeddedDatabaseFactoryBean();
            factory.setGenerateUniqueDatabaseName(false);
            factory.setDatabaseName(EmbeddedDatabaseFactory.DEFAULT_DATABASE_NAME + ";sql.syntax_mys=true");
            return factory;
        }
    }
}
