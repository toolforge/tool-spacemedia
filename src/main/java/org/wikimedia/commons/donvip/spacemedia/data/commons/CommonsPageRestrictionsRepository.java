package org.wikimedia.commons.donvip.spacemedia.data.commons;

import org.springframework.data.repository.CrudRepository;

public interface CommonsPageRestrictionsRepository extends CrudRepository<CommonsPageRestrictions, Integer> {

    boolean existsByPageAndType(CommonsPage page, String type);
}
