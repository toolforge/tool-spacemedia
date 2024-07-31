package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.service.dvids.DvidsService;

public record ApiSearchResult(String id, String unit_name) {

    public CompositeMediaId toCompositeMediaId(DvidsService dvids) {
        return new CompositeMediaId(dvids.getUnitAbbreviation(unit_name), id);
    }
}
