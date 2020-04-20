package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    @Autowired
    @Qualifier("domainEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    private Future<?> future;

    @PostConstruct
    public void init() {
        future = Search.getFullTextEntityManager(entityManagerFactory.createEntityManager()).createIndexer().start();
    }

    public void waitForInitialization() throws InterruptedException, ExecutionException {
        future.get();
    }
}
