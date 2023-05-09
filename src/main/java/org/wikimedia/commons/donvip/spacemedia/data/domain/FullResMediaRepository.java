package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FullResMediaRepository<T extends FullResMedia<ID, D>, ID, D extends Temporal>
        extends MediaRepository<T, ID, D> {

    long countByFullResMetadata_Phash(String phash);

    long countByFullResMetadata_Sha1(String sha1);

    List<T> findByFullResMetadata_AssetUrl(URL imageUrl);

    Optional<T> findByFullResMetadata_Phash(String phash);

    default List<T> findByMetadata_PhashOrFullResMetadata_Phash(String phash) {
        return findByMetadata_PhashOrFullResMetadata_Phash(phash, phash);
    }

    List<T> findByMetadata_PhashOrFullResMetadata_Phash(String phash, String fullResPhash);

    List<T> findByFullResMetadata_Sha1(String sha1);

    default List<T> findByMetadata_Sha1OrFullResMetadata_Sha1(String sha1) {
        return findByMetadata_Sha1OrFullResMetadata_Sha1(sha1, sha1);
    }

    List<T> findByMetadata_Sha1OrFullResMetadata_Sha1(String sha1, String fullResSha1);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames)))")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames)))")
    Page<T> findMissingInCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames)))")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)")
    List<T> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)")
    Page<T> findUploadedToCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)")
    long countUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.metadata.commonsFileNames) >= 2 or size (m.fullResMetadata.commonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    @Override
    @Modifying
    @Query("update #{#entityName} m set m.metadata.phash = null, m.fullResMetadata.phash = null where m.metadata.phash is not null or m.fullResMetadata.phash is not null")
    int resetPerceptualHashes();

    @Override
    @Modifying
    @Query("update #{#entityName} m set m.metadata.sha1 = null, m.fullResMetadata.sha1 = null where m.metadata.sha1 is not null or m.fullResMetadata.sha1 is not null")
    int resetSha1Hashes();
}
