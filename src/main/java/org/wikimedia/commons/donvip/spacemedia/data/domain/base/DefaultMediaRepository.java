package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface DefaultMediaRepository<T extends DefaultMedia> extends MediaRepository<T, CompositeMediaId> {

    // FIND

    @Query("select m from #{#entityName} m where m.id.repoId in ?1")
    Set<T> findAll(Set<String> repos);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1")
    Page<T> findAll(Set<String> repos, Pageable page);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    List<T> findByIgnoredTrue(Set<String> repos);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    Page<T> findByIgnoredTrue(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.id.repoId in ?1")
    List<T> findDuplicateInCommons(Set<String> repos);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    Page<T> findMissingImagesInCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.repoId in ?1")
    Page<T> findMissingImagesInCommons(Set<String> repos, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    Page<T> findMissingVideosInCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.repoId in ?1")
    Page<T> findMissingVideosInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    List<T> findMissingInCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<T> findMissingInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<T> findMissingInCommonsByDate(Set<String> repos, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.title = ?2")
    List<T> findMissingInCommonsByTitle(Set<String> repos, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    List<T> findUploadedToCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<T> findUploadedToCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1")
    Page<T> findByMetadata_PhashNotNull(Set<String> repos, Pageable page);
}
