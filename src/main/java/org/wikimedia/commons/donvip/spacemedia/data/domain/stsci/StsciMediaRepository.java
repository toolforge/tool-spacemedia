package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;

public interface StsciMediaRepository extends FullResMediaRepository<StsciMedia, String, ZonedDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "stsciCount", "stsciCountIgnored", "stsciCountMissing", "stsciCountMissingImages",
            "stsciCountMissingImagesByMission", "stsciCountMissingVideos", "stsciCountMissingVideosByMission",
            "stsciCountUploaded", "stsciFindByPhashNotNull" })
    @interface CacheEvictStsciAll {

    }

    @Override
    @CacheEvictStsciAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("stsciCount")
    long count();

    @Cacheable("stsciCountByMission")
    long countByMission(String mission);

    @Cacheable("stsciCountIgnored")
    long countByIgnoredTrueAndMission(String mission);

    @Cacheable("stsciCountMissing")
    @Query("select count(*) from #{#entityName} m where m.mission = ?1 and (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames)))")
    long countMissingInCommons(String mission);

    @Override
    @Cacheable("stsciCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Cacheable("stsciCountMissingImagesByMission")
    default long countMissingImagesInCommons(String mission) {
        return countMissingInCommons(mission);
    }

    @Override
    @Cacheable("stsciCountMissingVideos")
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Cacheable("stsciCountMissingVideosByMission")
    default long countMissingVideosInCommons(String mission) {
        return 0;
    }

    @Cacheable("stsciCountUploaded")
    @Query("select count(*) from #{#entityName} m where m.mission = ?1 and (exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames))")
    long countUploadedToCommons(String mission);

    @Cacheable("stsciCountPhashNotNullByMission")
    long countByMetadata_PhashNotNullAndMission(String mission);

    // FIND

    Set<StsciMedia> findAllByMission(String mission);

    Page<StsciMedia> findAllByMission(String mission, Pageable page);

    List<StsciMedia> findByIgnoredTrueAndMission(String mission);

    Page<StsciMedia> findByIgnoredTrueAndMission(String mission, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames)))")
    List<StsciMedia> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames)))")
    Page<StsciMedia> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames))) and m.mission = ?1")
    List<StsciMedia> findMissingInCommons(String mission);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and ((m.metadata.sha1 is not null and not exists elements (m.metadata.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResMetadata.commonsFileNames))) and m.mission = ?1")
    Page<StsciMedia> findMissingInCommons(String mission, Pageable page);

    @Override
    default Page<StsciMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    default Page<StsciMedia> findMissingImagesInCommons(String mission, Pageable page) {
        return findMissingInCommons(mission, page);
    }

    @Override
    default Page<StsciMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    default Page<StsciMedia> findMissingVideosInCommons(String mission, Pageable page) {
        return Page.empty();
    }

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)")
    List<StsciMedia> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)")
    Page<StsciMedia> findUploadedToCommons(Pageable page);

    @Query("select m from #{#entityName} m where (exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)) and m.mission = ?1")
    List<StsciMedia> findUploadedToCommons(String mission);

    @Query("select m from #{#entityName} m where (exists elements (m.metadata.commonsFileNames) or exists elements (m.fullResMetadata.commonsFileNames)) and m.mission = ?1")
    Page<StsciMedia> findUploadedToCommons(String mission, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.metadata.commonsFileNames) >= 2 or size (m.fullResMetadata.commonsFileNames) >= 2")
    List<StsciMedia> findDuplicateInCommons();

    @Query("select m from #{#entityName} m where (size (m.metadata.commonsFileNames) >= 2 or size (m.fullResMetadata.commonsFileNames) >= 2) and m.mission = ?1")
    List<StsciMedia> findDuplicateInCommons(String mission);

    @Override
    @Cacheable("stsciFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    Page<StsciMedia> findByMetadata_PhashNotNullAndMission(String mission, Pageable page);

    // SAVE

    @Override
    @CacheEvictStsciAll
    <S extends StsciMedia> S save(S entity);

    @Override
    @CacheEvictStsciAll
    <S extends StsciMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictStsciAll
    void deleteById(String id);

    @Override
    @CacheEvictStsciAll
    void delete(StsciMedia entity);

    @Override
    @CacheEvictStsciAll
    void deleteAll(Iterable<? extends StsciMedia> entities);

    @Override
    @CacheEvictStsciAll
    void deleteAll();
}
