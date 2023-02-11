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
public interface FullResExtraMediaRepository<T extends FullResExtraMedia<ID, D>, ID, D extends Temporal>
        extends FullResMediaRepository<T, ID, D> {

    long countByExtraMetadata_Phash(String phash);

    long countByExtraMetadata_Sha1(String sha1);

    List<T> findByExtraMetadata_AssetUrl(URL imageUrl);

    Optional<T> findByExtraMetadata_Phash(String phash);

    default List<T> findByMetadata_PhashOrFullResMetadata_PhashOrExtraMetadata_Phash(String phash) {
        return findByMetadata_PhashOrFullResMetadata_PhashOrExtraMetadata_Phash(phash, phash, phash);
    }

    List<T> findByMetadata_PhashOrFullResMetadata_PhashOrExtraMetadata_Phash(String phash, String fullResPhash, String extraPhash);

    List<T> findByExtraMetadata_Sha1(String sha1);

    default List<T> findByMetadata_Sha1OrFullResMetadata_Sha1OrExtraMetadata_Sha1(String sha1) {
        return findByMetadata_Sha1OrFullResMetadata_Sha1OrExtraMetadata_Sha1(sha1, sha1, sha1);
    }

    List<T> findByMetadata_Sha1OrFullResMetadata_Sha1OrExtraMetadata_Sha1(String sha1, String fullResSha1, String extraSha1);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.duplicates) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)) or (m.extraMetadata.sha1 is not null and not exists elements (m.extraCommonsFileNames)))")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.duplicates) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)) or (m.extraMetadata.sha1 is not null and not exists elements (m.extraCommonsFileNames)))")
    Page<T> findMissingInCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.duplicates) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)) or (m.extraMetadata.sha1 is not null and not exists elements (m.extraCommonsFileNames)))")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) or exists elements (m.fullResCommonsFileNames) or exists elements (m.extraCommonsFileNames)")
    List<T> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) or exists elements (m.fullResCommonsFileNames) or exists elements (m.extraCommonsFileNames)")
    Page<T> findUploadedToCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames) or exists elements (m.fullResCommonsFileNames) or exists elements (m.extraCommonsFileNames)")
    long countUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 or size (m.fullResCommonsFileNames) >= 2 or size (m.extraCommonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    @Override
    @Modifying
    @Query("update #{#entityName} m set m.metadata.phash = null, m.fullResMetadata.phash = null, m.extraMetadata.phash = null where m.metadata.phash is not null or m.fullResMetadata.phash is not null or m.extraMetadata.phash is not null")
    int resetPerceptualHashes();

    @Override
    @Modifying
    @Query("update #{#entityName} m set m.metadata.sha1 = null, m.fullResMetadata.sha1 = null, m.extraMetadata.sha1 = null where m.metadata.sha1 is not null or m.fullResMetadata.sha1 is not null or m.extraMetadata.sha1 is not null")
    int resetSha1Hashes();
}