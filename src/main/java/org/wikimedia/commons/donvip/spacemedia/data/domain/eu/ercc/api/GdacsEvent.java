package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record GdacsEvent(int GdacsEventId, String GdacsEventType, int Id) {

}
