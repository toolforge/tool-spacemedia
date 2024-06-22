package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask.Status;

public interface Video2CommonsTaskRepository extends CrudRepository<Video2CommonsTask, String> {

    List<Video2CommonsTask> findByStatusIn(Set<Status> states);

    Video2CommonsTask findFirstByUrlOrMetadataIdAndStatusInOrderByCreatedDesc(URL url, Long metadataId,
            Set<Status> states);

    @Query("select m from #{#entityName} m where m.status = 'FAIL' and not exists(select x from #{#entityName} x where x.status = 'DONE' and (x.url = m.url or (x.metadataId > 0 and x.metadataId = m.metadataId)))")
    List<Video2CommonsTask> findFailedTasks();
}
