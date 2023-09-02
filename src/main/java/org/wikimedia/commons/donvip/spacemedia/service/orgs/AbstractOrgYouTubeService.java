package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.execOutput;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.youtube.YouTubeApiService;
import org.wikimedia.commons.donvip.spacemedia.service.youtube.YouTubeMediaProcessor;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

public abstract class AbstractOrgYouTubeService extends AbstractOrgService<YouTubeMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgYouTubeService.class);

    private static final Pattern MERGING_DL = Pattern.compile("\\[ffmpeg\\] Merging formats into \"(\\S{11}\\.\\S{3,4})\"");
    private static final Pattern ALREADY_DL = Pattern.compile("\\[download\\] (\\S{11}\\.\\S{3,4}) has already been downloaded and merged");

    @Autowired
    private YouTubeApiService youtubeService;

    @Autowired
    private YouTubeMediaProcessor mediaProcessor;

    protected AbstractOrgYouTubeService(YouTubeMediaRepository repository, String id, Set<String> youtubeChannels) {
        super(repository, id, youtubeChannels);
    }

    @Override
    protected final Class<YouTubeMedia> getMediaClass() {
        return YouTubeMedia.class;
    }

    @Override
    public URL getSourceUrl(YouTubeMedia video, FileMetadata metadata) {
        return metadata.getAssetUrl();
    }

    @Override
    protected boolean includeByPerceptualHash() {
        return false;
    }

    protected void updateYouTubeVideos() {
        if (!videosEnabled) {
            LOGGER.info("Videos support disabled. Exiting...");
            return;
        }
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        for (String channelId : getRepoIds()) {
            count += updateYouTubeVideos(channelId);
        }
        syncYouTubeVideos();
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
                List<YouTubeMedia> videos = processYouTubeVideos(buildYouTubeVideoList(list, youtubeService.listVideos(list)));
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
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                processYouTubeVideos(repository.findAll(Set.of(channelId)));
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while fetching YouTube videos from channel " + channelId, e);
        }
        return count;
    }

    private List<YouTubeMedia> buildYouTubeVideoList(SearchListResponse searchList, VideoListResponse videoList) {
        return searchList.getItems().stream()
                .map(sr -> toYouTubeVideo(
                        videoList.getItems().stream().filter(v -> sr.getId().getVideoId().equals(v.getId())).findFirst().get()))
                .toList();
    }

    private YouTubeMedia toYouTubeVideo(Video ytVideo) {
        YouTubeMedia video = new YouTubeMedia();
        video.setId(new CompositeMediaId(null, ytVideo.getId()));
        addMetadata(video, newURL("https://www.youtube.com/watch?v=" + video.getId()), null);
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
        ofNullable(details.getDuration()).map(Duration::parse).ifPresent(video::setDuration);
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
                problem(getSourceUrl(video, video.getUniqueMetadata()), e);
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
        if (mediaService.updateMedia(video, getStringsToRemove(video), false, getUrlResolver(), true,
                includeByPerceptualHash(), ignoreExifMetadata(), path).getResult()) {
            save = true;
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
        try {
            String url = video.getUniqueMetadata().getAssetUrl().toExternalForm();
            String[] output = execOutput(List.of(
                "youtube-dl", "--no-progress", "--id", "--write-auto-sub", "--convert-subs", "srt", url),
                30, TimeUnit.MINUTES).split("\n");
            Optional<Matcher> matcher = Arrays.stream(output).map(ALREADY_DL::matcher).filter(Matcher::matches).findFirst();
            if (matcher.isEmpty()) {
                matcher = Arrays.stream(output).map(MERGING_DL::matcher).filter(Matcher::matches).findFirst();
            }
            if (matcher.isPresent()) {
                return Paths.get(matcher.get().group(1));
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.error("Error while downloading YouTube video: {}", e.getMessage());
        }
        return null;
    }

    @Override
    protected final int doUpload(YouTubeMedia video, boolean checkUnicity, Collection<FileMetadata> uploaded,
            boolean isManual) throws IOException {
        FileMetadata metadata = video.getUniqueMetadata();
        throw new UnsupportedOperationException("<h2>Spacemedia is not able to upload YouTube videos by itself.</h2>\n"
                + "<p>Please go to <a href=\"https://video2commons.toolforge.org/\">video2commons</a> and upload the <b>"
                + video.getId()
                + ".mkv/mp4/webm</b> file, using following information:</p>\n"
                + "<h4>Title:</h4>\n"
                + CommonsService.normalizeFilename(video.getUploadTitle(metadata))
                + "\n<h4>Wikicode:</h4>\n<pre>" + getWikiCode(video, metadata) + "</pre>");
    }

    @Override
    protected final YouTubeMedia refresh(YouTubeMedia video) throws IOException {
        return fillVideoSnippetAndDetails(video, youtubeService.getVideo(video.getId().getMediaId()));
    }

    protected boolean customProcessing(YouTubeMedia video) {
        return false;
    }

    private void syncYouTubeVideos() {
        List<String> categories = new ArrayList<>();
        categories.add("Spacemedia files (review needed)");
        categories.addAll(getOrgCategories());
        mediaProcessor.syncYouTubeVideos(repository.findMissingInCommons(getRepoIds()), categories);
    }

    protected abstract List<String> getOrgCategories();

    @Override
    public Set<String> findCategories(YouTubeMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            // To import by hand from personal account
            result.remove("Spacemedia files uploaded by " + commonsService.getAccount());
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(YouTubeMedia video) {
        Set<String> result = super.findLicenceTemplates(video);
        result.add("From YouTube |1= " + video.getId());
        result.add("YouTube CC-BY |1= " + video.getChannelTitle());
        result.add("LicenseReview");
        return result;
    }
}
