package org.wikimedia.commons.donvip.spacemedia.service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @Value("${search.enabled}")
    private boolean searchEnabled;

    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    public void checkSearchEnabled() {
        if (!searchEnabled) {
            throw new UnsupportedOperationException("Search is disabled");
        }
    }
}
