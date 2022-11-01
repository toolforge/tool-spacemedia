package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommonsOldImageRepository extends CrudRepository<CommonsOldImage, String> {

    List<CommonsOldImage> findBySha1In(Collection<String> sha1);

    boolean existsByName(String name);
}
