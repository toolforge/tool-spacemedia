package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import java.time.LocalDateTime;

import org.springframework.data.repository.NoRepositoryBean;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;

@NoRepositoryBean
public interface CommonEsoMediaRepository<T extends CommonEsoMedia>
        extends FullResMediaRepository<T, String, LocalDateTime> {

}
