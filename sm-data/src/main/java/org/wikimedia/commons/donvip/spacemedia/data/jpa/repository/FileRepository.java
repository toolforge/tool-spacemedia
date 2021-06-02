package org.wikimedia.commons.donvip.spacemedia.data.jpa.repository;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.File;

public interface FileRepository extends CrudRepository<File, String> {

}
