package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @Autowired
    private TransactionService transactionService;

    private Future<?> future;

    @PostConstruct
    public void init() {
        transactionService
                .doInTransaction(() -> future = Search.getFullTextEntityManager(entityManager).createIndexer().start());
    }

    public void waitForInitialization() throws InterruptedException, ExecutionException {
        future.get();
    }
}
