package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc.api;

import java.net.URL;
import java.time.LocalDateTime;

public record Request(String Access, URL Api, String Contact, int ContentItemId, String Countries, User CreatedByUser,
        LocalDateTime CreatedOnDate, LocalDateTime DeadlineDate, String DeliveryHour, String Description,
        URL DownloadPath, URL DownloadPath2, URL DownloadPath3, boolean Editable, String FileExplanation,
        String FileExplanation2, String FileExplanation3, String FileExtension, String FileExtension2,
        String FileExtension3, String FileName, String FileName2, String FileName3, String FileSize, String FileSize2,
        String FileSize3, boolean IsSecGen, User LastModifiedByUser, LocalDateTime LastModifiedOnDate, URL Link,
        String MapType, String OutputType, String ReasonForRequestChoice, String ReasonForRequestFree, String Response,
        boolean Restricted, String Status, String Title, boolean Urgent, User ValidatedByUser,
        LocalDateTime ValidatedOnDate, URL WebPath, URL WebPath2, URL WebPath3) {

}
