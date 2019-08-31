package org.wikimedia.commons.donvip.spacemedia.data.domain;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchConfiguration {

    @Autowired
    @Qualifier("domainEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Bean("searchEntityManager")
    public EntityManager searchEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @PostConstruct
    public void init() throws InterruptedException {
        Search.getFullTextEntityManager(searchEntityManager()).createIndexer().startAndWait();
    }
}
