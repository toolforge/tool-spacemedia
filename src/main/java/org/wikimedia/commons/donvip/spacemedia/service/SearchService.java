package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Lazy
@Service
public class SearchService {

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @Value("${search.enabled}")
    private boolean searchEnabled;

    @SuppressWarnings("unused")
    private Set<String> ignoredCommonTerms;

    @PostConstruct
    void init() throws IOException {
        ignoredCommonTerms = CsvHelper.loadSet(getClass().getResource("/search.ignored.terms.csv"));
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
