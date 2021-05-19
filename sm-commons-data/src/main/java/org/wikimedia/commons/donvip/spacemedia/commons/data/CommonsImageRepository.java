package org.wikimedia.commons.donvip.spacemedia.commons.data;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommonsImageRepository extends CrudRepository<CommonsImage, String> {

    List<CommonsImage> findBySha1(String sha1);
}
