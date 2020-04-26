package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.math.BigInteger;
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

    long countByFullResMetadata_Phash(BigInteger phash);

    long countByFullResMetadata_Sha1(String sha1);

    List<T> findByFullResMetadata_AssetUrl(URL imageUrl);

    Optional<T> findByFullResMetadata_Phash(BigInteger phash);

    default List<T> findByMetadata_PhashOrFullResMetadata_Phash(BigInteger phash) {
        return findByMetadata_PhashOrFullResMetadata_Phash(phash, phash);
    }

    List<T> findByMetadata_PhashOrFullResMetadata_Phash(BigInteger phash, BigInteger fullResPhash);

    Optional<T> findByFullResMetadata_Sha1(String sha1);

    default List<T> findByMetadata_Sha1OrFullResMetadata_Sha1(String sha1) {
        return findByMetadata_Sha1OrFullResMetadata_Sha1(sha1, sha1);
    }

    List<T> findByMetadata_Sha1OrFullResMetadata_Sha1(String sha1, String fullResSha1);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
    Page<T> findMissingInCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
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
