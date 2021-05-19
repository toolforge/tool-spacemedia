package org.wikimedia.commons.donvip.spacemedia.service;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.VideoListResponse;

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final YouTube youtube;

    public YouTubeService(@Value("${youtube.app.name:Spacemedia}") String applicationName)
            throws GeneralSecurityException, IOException {
        youtube = new YouTube.Builder(newTrustedTransport(), JacksonFactory.getDefaultInstance(), request -> {
        }).setApplicationName(applicationName).build();
    }

    private <T> T executeRequest(YouTubeRequest<T> request) throws IOException {
        return request.setKey(apiKey).execute();
    }

    public SearchListResponse searchVideos(String channelId, String pageToken) throws IOException {
        return executeRequest(youtube.search().list("snippet").setType("video").setVideoLicense("creativeCommon")
                .setChannelId(channelId).setMaxResults(50L).setPageToken(pageToken));
    }

    public VideoListResponse listVideos(SearchListResponse searchList) throws IOException {
        return listVideos(searchList.getItems().stream().map(SearchResult::getId).map(ResourceId::getVideoId)
                .collect(Collectors.toList()));
    }

    public VideoListResponse listVideos(Iterable<String> videoIds) throws IOException {
        return executeRequest(youtube.videos().list("contentDetails,snippet").setId(String.join(",", videoIds)));
    }
}
