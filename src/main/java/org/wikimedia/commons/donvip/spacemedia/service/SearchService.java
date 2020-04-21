package org.wikimedia.commons.donvip.spacemedia.service;

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

    @PostConstruct
    public void init() {
        transactionService.doInTransaction(() -> {
            try {
                Search.getFullTextEntityManager(entityManager).createIndexer().startAndWait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
