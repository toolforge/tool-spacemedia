package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record Continent(String CC, double East, String Flag, double LatitudeAverage, double LongitudeAverage,
        String Name, double North, double South, double West) {

}
