package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FullResMediaRepository<T extends FullResMedia<ID, D>, ID, D extends Temporal>
        extends MediaRepository<T, ID, D> {

    long countByFullResSha1(String sha1);

    List<T> findByFullResAssetUrl(URL imageUrl);

    Optional<T> findByFullResSha1(String sha1);

    default Optional<T> findBySha1OrFullResSha1(String sha1) {
        return findBySha1OrFullResSha1(sha1, sha1);
    }

    Optional<T> findBySha1OrFullResSha1(String sha1, String fullRessha1);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResSha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResSha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
    Page<T> findMissingInCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResSha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) or exists elements (m.fullResCommonsFileNames)")
    List<T> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) or exists elements (m.fullResCommonsFileNames)")
    Page<T> findUploadedToCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames) or exists elements (m.fullResCommonsFileNames)")
    long countUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 or size (m.fullResCommonsFileNames) >= 2")
    List<T> findDuplicateInCommons();
}
