package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface StsciMediaRepository extends MediaRepository<StsciMedia, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "stsciCount", "stsciCountIgnored", "stsciCountMissing", "stsciCountMissingImages",
            "stsciCountMissingImagesByMission", "stsciCountMissingVideos", "stsciCountMissingVideosByMission",
            "stsciCountUploaded" })
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
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where m.mission = ?1 and (m.ignored is null or m.ignored is false) and md.sha1 is not null and not exists elements (md.commonsFileNames)")
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
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where m.mission = ?1 and exists elements (md.commonsFileNames)")
    long countUploadedToCommons(String mission);

    @Cacheable("stsciCountPhashNotNullByMission")
    long countByMetadata_PhashNotNullAndMission(String mission);

    // FIND

    Set<StsciMedia> findAllByMission(String mission);

    Page<StsciMedia> findAllByMission(String mission, Pageable page);

    List<StsciMedia> findByIgnoredTrueAndMission(String mission);

    Page<StsciMedia> findByIgnoredTrueAndMission(String mission, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where m.mission = ?1 and (m.ignored is null or m.ignored is false) and md.sha1 is not null and not exists elements (md.commonsFileNames)")
    List<StsciMedia> findMissingInCommons(String mission);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where m.mission = ?1 and (m.ignored is null or m.ignored is false) and md.sha1 is not null and not exists elements (md.commonsFileNames)")
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

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.mission = ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<StsciMedia> findMissingInCommonsByDate(String mission, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.mission = ?1 and m.title = ?2")
    List<StsciMedia> findMissingInCommonsByTitle(String mission, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where m.mission = ?1 and exists elements (md.commonsFileNames)")
    List<StsciMedia> findUploadedToCommons(String mission);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where m.mission = ?1 and exists elements (md.commonsFileNames)")
    Page<StsciMedia> findUploadedToCommons(String mission, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where m.mission = ?1 and size (md.commonsFileNames) >= 2")
    List<StsciMedia> findDuplicateInCommons(String mission);

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
