package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SearchService {

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @Autowired
    @Qualifier("domainTransactionManager")
    protected PlatformTransactionManager txManager;

    private Future<?> future;

    @PostConstruct
    public void init() {
        new TransactionTemplate(txManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                future = Search.getFullTextEntityManager(entityManager).createIndexer().start();
            }
        });
    }

    public void waitForInitialization() throws InterruptedException, ExecutionException {
        future.get();
    }
}
