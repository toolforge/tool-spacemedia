package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommonsImageRepository extends CrudRepository<CommonsImage, String> {

    List<CommonsImage> findBySha1OrderByTimestamp(String sha1);
}
