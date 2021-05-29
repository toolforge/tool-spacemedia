package org.wikimedia.commons.donvip.spacemedia.commons.data.jpa;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommonsFileArchiveRepository extends CrudRepository<CommonsFileArchive, Integer> {

    /**
     * Retrieves deleted files by their base36 SHA-1. This method suffers
     * a major performance bug on the database replica (the field is no indexed,
     * see <a href="https://phabricator.wikimedia.org/T71088">#T71088</a>
     * 
     * @param sha1 base36 SHA-1, zero padded to 31 characters
     * @return list of deleted files matching the provided hash
     */
    List<CommonsFileArchive> findBySha1(String sha1);
}
