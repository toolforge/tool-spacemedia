package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties("ISO3")
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record Country(String Capital, String ContinentCC, String ContinentName, double East, String Flag, String Flag32,
        boolean IsMemberState, String Iso2, String Iso3, boolean IsUCPM, double LatitudeAverage,
        URL Link, double LongitudeAverage, String Name, double North, double South, List<TimeZone> TimeZones,
        double West) {

}
