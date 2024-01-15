package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record GetPagedMapsResponse(List<MapsItem> Items, String LastItemIdentifier, int NumberOfPages,
        int PageIndex, int TotalCount) {
}
