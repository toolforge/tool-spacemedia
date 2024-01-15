package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record DailyMapRequest(List<String> Attachments, String ClosedByUser, String ClosedOnDate, int ContentItemId,
        Continent Continent, List<Country> Countries, Country Country, User CreatedByUser, LocalDateTime CreatedOnDate,
        LocalDateTime DeadlineDate, String DeadlineHour, String Description, List<String> Drafts,
        List<String> EventTypes, User LastModifiedByUser, LocalDateTime LastModifiedOnDate, String LatestDraft,
        URL Link, String PreparedBy, String Response, String Status, List<String> StatusChangeHistory, String Title,
        User ValidatedByUser, LocalDateTime ValidatedOnDate) {

}
