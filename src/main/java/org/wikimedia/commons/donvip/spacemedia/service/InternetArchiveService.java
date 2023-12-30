package org.wikimedia.commons.donvip.spacemedia.service;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Service
public class InternetArchiveService {

    private static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyyMMddHHmmss",
            Locale.ENGLISH);

    @Autowired
    private ObjectMapper jackson;

    public Optional<URL> retrieveOldestUrl(URL url) throws IOException {
        return retrieveOldestUrl(url.toExternalForm());
    }

    public Optional<URL> retrieveOldestUrl(String url) throws IOException {
        Snapshot snapshot = jackson.readValue(
                newURL("https://archive.org/wayback/available?timestamp=19900101&url=" + url),
                WaybackMachineResponse.class).archived_snapshots().closest();
        return snapshot != null && snapshot.available() ? Optional.of(snapshot.url()) : Optional.empty();
    }

    public ZonedDateTime extractTimestamp(String url) {
        return LocalDateTime.parse(url.split("/")[4], TIMESTAMP_PATTERN).atZone(ZoneId.systemDefault());
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static record WaybackMachineResponse(URL url, ArchivedSnapshots archived_snapshots, String timestamp) {
    }

    private static record ArchivedSnapshots(Snapshot closest) {
    }

    private static record Snapshot(boolean available, URL url, String timestamp, int status) {
    }
}
