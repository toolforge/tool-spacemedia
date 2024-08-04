package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.service.dvids.DvidsService;

public record ApiSearchResult(String id, String unit_name) {

    public CompositeMediaId toCompositeMediaId(String unit, DvidsService dvids) {
        return new CompositeMediaId("*".equals(unit) || isBlank(unit) ? dvids.getUnitAbbreviation(unit_name) : unit,
                id);
    }
}
