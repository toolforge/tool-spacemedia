package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommonsCategoryLinkRepository extends CrudRepository<CommonsCategoryLink, CommonsCategoryLinkId> {

    List<CommonsCategoryLink> findByTypeAndIdTo(CommonsCategoryLinkType type, String to);
}
