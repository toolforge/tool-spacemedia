package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record RelatedEchoFlash(URL Api, int ContentItemID, String CountryName, String Description,
        EventType EventType, URL EventTypeIconLink, String EventTypeName, List<EventType> EventTypes, URL Link,
        LocalDateTime PublishedOnDate, String Title) {

}
