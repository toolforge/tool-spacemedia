package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;

public record ApiSearchResult(String id, String unit) {

    public CompositeMediaId toCompositeMediaId() {
        return new CompositeMediaId(unit, id);
    }
}
