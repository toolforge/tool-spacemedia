package org.wikimedia.commons.donvip.spacemedia.service.youtube;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

@Lazy
@Service
@SuppressWarnings("deprecation")
public class YouTubeApiService {

    private static final String CREATIVE_COMMON = "creativeCommon";

    @Value("${youtube.api.key}")
    private String apiKey;

    private final YouTube youtube;

    public YouTubeApiService(@Value("${youtube.app.name:Spacemedia}") String applicationName)
            throws GeneralSecurityException, IOException {
        // Google wants us to use com.google.api.client.json.GsonFactory instead, I don't
        youtube = new YouTube.Builder(newTrustedTransport(), JacksonFactory.getDefaultInstance(), request -> {
        }).setApplicationName(applicationName).build();
    }

    private <T> T executeRequest(YouTubeRequest<T> request) throws IOException {
        return request.setKey(apiKey).execute();
    }

    private YouTube.Search.List searchVideos(String channelId, String pageToken) throws IOException {
        return youtube.search().list(List.of("snippet")).setType(List.of("video")).setChannelId(channelId)
                .setMaxResults(50L)
                .setPageToken(pageToken);
    }

    public SearchListResponse searchCreativeCommonsVideos(String channelId, String pageToken) throws IOException {
        return executeRequest(searchVideos(channelId, pageToken).setVideoLicense(CREATIVE_COMMON));
    }

    public SearchListResponse searchVideos(String channelId, String pageToken, String licenseText) throws IOException {
        return executeRequest(searchVideos(channelId, pageToken).setQ(licenseText));
    }

    public VideoListResponse listVideos(SearchListResponse searchList) throws IOException {
        return listVideos(searchList.getItems().stream().map(SearchResult::getId).map(ResourceId::getVideoId).toList());
    }

    public VideoListResponse listVideos(List<String> videoIds) throws IOException {
        return videoIds.isEmpty() ? new VideoListResponse()
                : executeRequest(youtube.videos().list(List.of("contentDetails", "snippet", "status")).setId(videoIds));
    }

    public Video getVideo(String videoId) throws IOException {
        return listVideos(List.of(videoId)).getItems().get(0);
    }

    public static boolean isCreativeCommons(Video video) {
        return CREATIVE_COMMON.equals(video.getStatus().getLicense());
    }
}
