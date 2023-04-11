package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;

@NoRepositoryBean
public interface DjangoplicityMediaRepository<T extends DjangoplicityMedia>
        extends FullResMediaRepository<T, String, LocalDateTime> {

    // COUNT

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.duplicates) and ((m.metadata.sha1 is not null and not exists elements (m.commonsFileNames)) or (m.fullResMetadata.sha1 is not null and not exists elements (m.fullResCommonsFileNames)))")
    long countMissingImagesInCommons();

    @Override
    @Query(value = "select 0", nativeQuery = true)
    long countMissingVideosInCommons();

    // FIND

    @Override
    default Page<T> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<T> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }
}
