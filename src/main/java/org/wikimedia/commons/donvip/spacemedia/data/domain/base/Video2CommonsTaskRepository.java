package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask.Status;

public interface Video2CommonsTaskRepository extends CrudRepository<Video2CommonsTask, String> {

    List<Video2CommonsTask> findByStatusIn(Set<Status> states);

    @Query("select m from #{#entityName} m where (m.url = ?1 or m.metadataId = ?2) and m.status in ?3 order by m.created desc limit 1")
    Video2CommonsTask findFirstByUrlOrMetadataIdAndStatusInOrderByCreatedDesc(URL url, Long metadataId,
            Set<Status> states);

    @Query("select m from #{#entityName} m where m.status = 'FAIL' and not exists(select x from #{#entityName} x where x.status IN ('DONE','PROGRESS') and (x.url = m.url or x.metadataId = m.metadataId))")
    List<Video2CommonsTask> findFailedTasks();
}
