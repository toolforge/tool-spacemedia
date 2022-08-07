package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface HashAssociationRepository extends CrudRepository<HashAssociation, String> {

    @Query("select distinct sha1 from HashAssociation where phash = ?1")
    List<String> findSha1ByPhash(String phash);
}
