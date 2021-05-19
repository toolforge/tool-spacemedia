package org.wikimedia.commons.donvip.spacemedia.commons.data;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface CommonsPageRepository extends CrudRepository<CommonsPage, Integer> {

    Optional<CommonsPage> findByNamespaceAndTitle(int namespace, String title);

    default Optional<CommonsPage> findByCategoryTitle(String title) {
        return findByNamespaceAndTitle(14, title);
    }
}
