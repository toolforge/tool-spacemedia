package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Video2CommonsTask.Status;

public interface Video2CommonsTaskRepository extends CrudRepository<Video2CommonsTask, String> {

    List<Video2CommonsTask> findByStatusIn(Set<Status> states);
}
