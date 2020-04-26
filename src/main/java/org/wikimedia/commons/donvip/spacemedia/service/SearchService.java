package org.wikimedia.commons.donvip.spacemedia.service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @Autowired
    private TransactionService transactionService;

    @Value("${search.enabled}")
    private boolean searchEnabled;

    @PostConstruct
    public void init() {
        if (searchEnabled) {
            transactionService.doInTransaction(() -> {
                try {
                    // https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/?v=5.11#search-batchindexing-threadsandconnections
                    // threads = typesToIndexInParallel * (threadsToLoadObjects + 1)
                    Search.getFullTextEntityManager(entityManager).createIndexer()
                            .progressMonitor(new SimpleIndexingProgressMonitor(1000)).threadsToLoadObjects(7)
                            .startAndWait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    public void checkSearchEnabled() {
        if (!searchEnabled) {
            throw new UnsupportedOperationException("Search is disabled");
        }
    }
}
