package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateContext;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.service.youtube.YouTubeApiService;
import org.wikimedia.commons.donvip.spacemedia.service.youtube.YouTubeMediaProcessor;
import org.wikimedia.commons.donvip.spacemedia.utils.MediaUtils;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

public abstract class AbstractOrgYouTubeService extends AbstractOrgService<YouTubeMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgYouTubeService.class);

    @Lazy
    @Autowired
    private YouTubeApiService youtubeService;

    @Lazy
    @Autowired
    private YouTubeMediaProcessor mediaProcessor;

    private final Map<String, String> userNamesByRepoIds;

    protected AbstractOrgYouTubeService(YouTubeMediaRepository repository, String id, Set<String> youtubeChannels) {
        super(repository, id, youtubeChannels.stream().map(x -> x.split(":")[1]).collect(toSet()));
        userNamesByRepoIds = youtubeChannels.stream().map(x -> x.split(":")).collect(toMap(x -> x[1], x -> x[0]));
    }

    @Override
    protected final Class<YouTubeMedia> getMediaClass() {
        return YouTubeMedia.class;
    }

    @Override
    protected String getSource(YouTubeMedia media, FileMetadata metadata) {
        return "{{From YouTube |1= " + media.getIdUsedInOrg() + "}}";
    }

    @Override
    public URL getSourceUrl(YouTubeMedia video, FileMetadata metadata, String ext) {
        return metadata.getAssetUrl();
    }

    @Override
    protected boolean includeByPerceptualHash() {
        return false;
    }

    @Override
    public String getUiRepoId(String repoId) {
        return userNamesByRepoIds.get(repoId);
    }

    @Override
    public void updateMedia(String[] args) {
        if (!videosEnabled) {
            LOGGER.info("Videos support disabled. Exiting...");
            return;
        }
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        Set<String> repoIdsFromArgs = getRepoIdsFromArgs(args);
        for (String channelId : repoIdsFromArgs) {
            count += updateYouTubeVideos(channelId);
        }

        List<String> categories = new ArrayList<>();
        categories.addAll(getReviewCategories(null));
        categories.addAll(getOrgCategories());
        mediaProcessor.syncYouTubeVideos(repository.findMissingInCommons(repoIdsFromArgs), categories);

        endUpdateMedia(count, emptyList(), emptyList(), start);
    }

    private int updateYouTubeVideos(String channelId) {
        int count = 0;
        try {
            LOGGER.info("Fetching YouTube videos from channel '{}'...", channelId);
            List<YouTubeMedia> freeVideos = new ArrayList<>();
            String pageToken = null;
            do {
                SearchListResponse list = youtubeService.searchCreativeCommonsVideos(channelId, pageToken);
                pageToken = list.getNextPageToken();
                List<YouTubeMedia> videos = processYouTubeVideos(
                        buildYouTubeVideoList(channelId, list, youtubeService.listVideos(list)));
                count += videos.size();
                freeVideos.addAll(videos);
            } while (pageToken != null);
            LOGGER.info("Processed {} free videos for channel {}", count, channelId);
            if (!freeVideos.isEmpty()) {
                Set<YouTubeMedia> noLongerFreeVideos = repository.findNotIn(Set.of(channelId),
                        freeVideos.stream().map(v -> v.getId().getMediaId()).collect(toSet()));
                if (!noLongerFreeVideos.isEmpty()) {
                    LOGGER.warn("Deleting {} videos no longer-free for channel {}: {}",
                            noLongerFreeVideos.size(), channelId, noLongerFreeVideos);
                    repository.deleteAll(noLongerFreeVideos);
                    count += noLongerFreeVideos.size();
                }
            }
        } catch (HttpClientErrorException e) {
            LOGGER.error("HttpClientError while fetching YouTube videos from channel {}: {}", channelId, e.getMessage());
            GlitchTip.capture(e);
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                processYouTubeVideos(repository.findAll(Set.of(channelId)));
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while fetching YouTube videos from channel " + channelId, e);
            GlitchTip.capture(e);
        }
        return count;
    }

    private List<YouTubeMedia> buildYouTubeVideoList(String channel, SearchListResponse searchList,
            VideoListResponse videoList) {
        return searchList.getItems().stream()
                .map(sr -> toYouTubeVideo(channel,
                        videoList.getItems().stream().filter(v -> sr.getId().getVideoId().equals(v.getId())).findFirst().get()))
                .toList();
    }

    private YouTubeMedia toYouTubeVideo(String channel, Video ytVideo) {
        YouTubeMedia video = new YouTubeMedia();
        video.setId(new CompositeMediaId(channel, ytVideo.getId()));
        addMetadata(video, "https://www.youtube.com/watch?v=" + video.getIdUsedInOrg(), fm -> {
            fm.setExtension("webm");
            ofNullable(ytVideo.getContentDetails().getDuration()).map(Duration::parse).ifPresent(fm.getMediaDimensions()::setDuration);
        });
        return fillVideoSnippetAndDetails(video, ytVideo);
    }

    private static YouTubeMedia fillVideoSnippetAndDetails(YouTubeMedia video, Video ytVideo) {
        VideoSnippet snippet = ytVideo.getSnippet();
        ofNullable(snippet.getChannelId()).ifPresent(video.getId()::setRepoId);
        ofNullable(snippet.getChannelTitle()).ifPresent(video::setChannelTitle);
        ofNullable(snippet.getPublishedAt()).map(DateTime::toStringRfc3339).map(ZonedDateTime::parse)
                .ifPresent(video::setPublicationDateTime);
        ofNullable(snippet.getDescription()).ifPresent(video::setDescription);
        ofNullable(getBestThumbnailUrl(snippet.getThumbnails())).ifPresent(video::setThumbnailUrl);
        ofNullable(snippet.getTitle()).ifPresent(video::setTitle);
        VideoContentDetails details = ytVideo.getContentDetails();
        ofNullable(details.getCaption()).map(Boolean::valueOf).ifPresent(video::setCaption);
        return video;
    }

    private static URL getBestThumbnailUrl(ThumbnailDetails td) {
        return td != null
                ? newURL(ObjectUtils.firstNonNull(td.getMaxres(), td.getHigh(), td.getMedium(), td.getDefault())
                        .getUrl())
                : null;
    }

    private List<YouTubeMedia> processYouTubeVideos(Iterable<YouTubeMedia> videos) {
        List<YouTubeMedia> result = new ArrayList<>();
        for (YouTubeMedia video : videos) {
            try {
                result.add(processYouTubeVideo(video));
            } catch (IOException e) {
                problem(getSourceUrl(video, video.getUniqueMetadata(), video.getUniqueMetadata().getExtension()), e);
            }
        }
        return result;
    }

    private YouTubeMedia processYouTubeVideo(YouTubeMedia video) throws IOException {
        boolean save = false;
        Optional<YouTubeMedia> optVideoInRepo = repository.findById(video.getId());
        if (optVideoInRepo.isPresent()) {
            video = optVideoInRepo.get();
        } else {
            save = true;
        }
        Path path = !video.getUniqueMetadata().hasSha1() ? downloadVideo(video) : null;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            if (mediaService.updateMedia(
                    new MediaUpdateContext<>(video, path, getUrlResolver(), httpClient, null, false,
                            ignoreExifMetadata()),
                    getPatternsToRemove(video), getStringsToRemove(video), this::getSimilarUploadedMediaByDate,
                    checkAllowlist(), checkBlocklist(), includeByPerceptualHash()).result()) {
                save = true;
            }
        }
        if (path != null) {
            Files.deleteIfExists(path);
        }
        if (customProcessing(video)) {
            save = true;
        }
        return save ? repository.save(video) : video;
    }

    private Path downloadVideo(YouTubeMedia video) {
        return MediaUtils.downloadYoutubeVideo(video.getUniqueMetadata().getAssetUrl().toExternalForm());
    }

    @Override
    protected final YouTubeMedia refresh(YouTubeMedia video) throws IOException {
        return fillVideoSnippetAndDetails(video, youtubeService.getVideo(video.getId().getMediaId()));
    }

    protected boolean customProcessing(YouTubeMedia video) {
        return false;
    }

    protected abstract List<String> getOrgCategories();

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "YouTube files uploaded by " + commonsService.getAccount();
    }

    @Override
    public Set<String> findLicenceTemplates(YouTubeMedia video, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(video, metadata);
        result.add("YouTube CC-BY |1= " + video.getChannelTitle());
        result.add("LicenseReview");
        return result;
    }
}
