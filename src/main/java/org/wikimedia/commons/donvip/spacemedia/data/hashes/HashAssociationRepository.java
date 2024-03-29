package org.wikimedia.commons.donvip.spacemedia.data.hashes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface HashAssociationRepository extends CrudRepository<HashAssociation, String> {

    @Query("select distinct sha1 from HashAssociation where phash = ?1 and mime = ?2")
    List<String> findSha1ByPhashAndMime(String phash, String mime);

    @Query("select distinct sha1 from HashAssociation where length(sha1) = 40")
    List<String> findObsoleteSha1();

    Optional<HashAssociation> findBySha1AndMimeIsNull(String sha1);
}
