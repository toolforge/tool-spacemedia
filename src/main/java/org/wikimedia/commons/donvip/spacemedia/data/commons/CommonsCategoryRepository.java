package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface CommonsCategoryRepository extends CrudRepository<CommonsCategory, Integer> {

    Optional<CommonsCategory> findByTitle(String title);
}
