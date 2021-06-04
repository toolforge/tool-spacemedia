package org.wikimedia.commons.donvip.spacemedia.data.jpa.repository;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

public interface FilePublicationRepository extends CrudRepository<FilePublication, PublicationKey> {

    Optional<FilePublication> findByUrl(URL url);

    List<FilePublication> findByFileNullOrderByIdId();
}
