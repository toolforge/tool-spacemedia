package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import org.wikimedia.commons.donvip.spacemedia.utils.FlexibleDoubleDeserializer;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record EchoFlash(URL Api, LocalDateTime ApprovedByErccOnDate, int ContentItemId, Continent Continent,
        List<Country> Countries, Country Country, User CreatedByUser, LocalDateTime CreatedOnDate, String Description,
        String DescriptionJRC, int DisplayOrder, List<Emergency> Emergencies, EventType EventType,
        String EventTypeCode, List<EventType> EventTypes, List<String> GdacsEvents, String Glide,
        boolean IsCopernicusActivated, boolean IsFromJrc, String Iso3, Boolean IsUcpmActivated,
        List<String> ItemSources, User LastModifiedByUser, LocalDateTime LastModifiedOnDate,
        @JsonDeserialize(using = FlexibleDoubleDeserializer.class) double Latitude,
        URL Link, @JsonDeserialize(using = FlexibleDoubleDeserializer.class) double Longitude, List<String> Losses,
        List<String> AffectedAreas, String NextEchoFlashID, String PreviousEchoFlashID,
        String PublicationStatus, LocalDateTime PublishedOnDate, RelatedEchoFlash RelatedEchoFlash,
        String RelatedEchoFlashID, String Scope, String Section, String SourceList, String Sources, String SourcesJRC,
        String Title, User ValidatedByErccUser, String ValidatedByErccUserId) {
}
