package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.EchoMapType;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record MapsItem(String Access, URL Api, String CategoryCode, int ContentItemId, Continent Continent,
        List<Country> Countries, Country Country, String CreatedByUser, LocalDateTime CreatedOnDate,
        DailyMapRequest DailyMapRequest, String Description, EchoFlash EchoFlash, List<Emergency> Emergencies,
        List<EventType> EventTypes, String FileSize, String FileSize2, List<GdacsEvent> GdacsEvents, String Glide,
        URL ImageDownloadPath, String ImageExtension, URL ImageWebPath, String Iso3, List<ItemSource> ItemSources,
        String LastModifiedByUser, LocalDateTime LastModifiedOnDate, URL Link, URL MainDownloadPath,
        String MainFileExtension, String MainFileName, URL MainWebPath, LocalDateTime MapOf, String MapType,
        String Owner, String PreparedBy, LocalDateTime PublishedOnDate, Request Request, String RequestID,
        URL SecondDownloadPath, String SecondFileExtension, String SecondFileName, URL SecondWebPath,
        String SourceList, String Sources, URL ThumbnailDownloadPath, String ThumbnailExtension,
        String ThumbnailFileName, String ThumbnailFileSize, URL ThumbnailWebPath, String Title) {

    public EchoMapType getEchoMapType() {
        return EchoMapType.valueOf(MapType().replace(" Map", "").replace(" map", "").replace(' ', '_'));
    }
}
