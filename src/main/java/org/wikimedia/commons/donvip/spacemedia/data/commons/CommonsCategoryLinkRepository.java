package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CommonsCategoryLinkRepository extends CrudRepository<CommonsCategoryLink, CommonsCategoryLinkId> {

    @Query("select count(*) from CommonsCategoryLink cl where cl.type = ?1 and cl.id.to = ?2")
    long countByTypeAndIdTo(CommonsCategoryLinkType type, String to);

    @Query("select cl.id from CommonsCategoryLink cl where cl.type = ?1 and cl.id.to = ?2")
    Set<CommonsCategoryLinkId> findIdByTypeAndIdTo(CommonsCategoryLinkType type, String to);

    @Query(value = "select cl.id from CommonsCategoryLink cl where cl.type = ?1 and cl.id.to = ?2",
            countQuery = "select count(*) from CommonsCategoryLink cl where cl.type = ?1 and cl.id.to = ?2")
    Page<CommonsCategoryLinkId> findIdByTypeAndIdTo(CommonsCategoryLinkType type, String to, Pageable page);

    Set<CommonsCategoryLink> findByIdFrom(CommonsPage from);
}
