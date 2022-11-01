package org.wikimedia.commons.donvip.spacemedia.data.commons;

import org.springframework.data.repository.CrudRepository;

public interface CommonsRevisionRepository extends CrudRepository<CommonsRevision, Integer> {

    int countByPage(CommonsPage page);
}
