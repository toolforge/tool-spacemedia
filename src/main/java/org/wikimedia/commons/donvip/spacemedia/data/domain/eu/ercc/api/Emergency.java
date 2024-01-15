package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record Emergency(String CecisEmergency, int ContentItemId, Continent Continent, int CountDown,
        List<Country> Countries, Country Country, User CreatedByUser, LocalDateTime CreatedOnDate,
        LocalDateTime DeadLine, String Description, String EmergencyLevel, LocalDateTime EndDateTime,
        EventType EventType, User LastModifiedByUser, LocalDateTime LastModifiedOnDate, URL Link, Location Location,
        int ModuleID, String PublicDescription, LocalDateTime StartDateTime, String Status, String SubTitle,
        String Title) {

}
