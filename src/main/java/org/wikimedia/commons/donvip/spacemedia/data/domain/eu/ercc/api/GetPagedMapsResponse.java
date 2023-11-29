package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.util.List;

public record GetPagedMapsResponse(List<MapsItem> Items, String LastItemIdentifier, int NumberOfPages,
        int PageIndex, int TotalCount) {
}