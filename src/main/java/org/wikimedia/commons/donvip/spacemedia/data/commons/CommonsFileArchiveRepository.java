package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommonsFileArchiveRepository extends CrudRepository<CommonsFileArchive, Integer> {

    List<CommonsFileArchive> findBySha1(String sha1);
}
