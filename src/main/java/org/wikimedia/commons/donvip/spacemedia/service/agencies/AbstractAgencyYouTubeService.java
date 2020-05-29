package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.service.YouTubeService;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

public abstract class AbstractAgencyYouTubeService
        extends AbstractAgencyService<YouTubeVideo, String, Instant, YouTubeVideo, String, Instant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyYouTubeService.class);

    private static final Pattern MERGING_DL = Pattern.compile("\\[ffmpeg\\] Merging formats into \"(\\S{11}\\.\\S{3,4})\"");
    private static final Pattern ALREADY_DL = Pattern.compile("\\[download\\] (\\S{11}\\.\\S{3,4}) has already been downloaded and merged");

    @Autowired
    protected YouTubeVideoRepository youtubeRepository;
    @Autowired
    private YouTubeService youtubeService;

    protected final Set<String> youtubeChannels;

    public AbstractAgencyYouTubeService(YouTubeVideoRepository repository, String id, Set<String> youtubeChannels) {
        super(repository, id);
        this.youtubeChannels = Objects.requireNonNull(youtubeChannels);
    }

    @Override
    protected final Class<YouTubeVideo> getMediaClass() {
        return YouTubeVideo.class;
    }

    @Override
    public URL getSourceUrl(YouTubeVideo video) throws MalformedURLException {
        return video.getMetadata().getAssetUrl();
    }

    @Override
    protected final Optional<Temporal> getUploadDate(YouTubeVideo video) {
        return Optional.of(video.getDate());
    }

    protected void updateYouTubeVideos() {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        for (String channelId : youtubeChannels) {
            try {
                LOGGER.info("Fetching YouTube videos from channel '{}'...", channelId);
                List<YouTubeVideo> freeVideos = new ArrayList<>();
                String pageToken = null;
                do {
                    SearchListResponse list = youtubeService.searchVideos(channelId, pageToken);
                    pageToken = list.getNextPageToken();
                    List<YouTubeVideo> videos = processYouTubeVideos(buildYouTubeVideoList(list, youtubeService.listVideos(list)));
                    count += videos.size();
                    freeVideos.addAll(videos);
                } while (pageToken != null);
                if (!freeVideos.isEmpty()) {
                    Set<YouTubeVideo> noLongerFreeVideos = youtubeRepository.findAll(Set.of(channelId));
                    noLongerFreeVideos.removeAll(freeVideos);
                    if (!noLongerFreeVideos.isEmpty()) {
                        LOGGER.warn("Deleting {} videos no longer-free for channel {}: {}",
                                noLongerFreeVideos.size(), channelId, noLongerFreeVideos);
                        youtubeRepository.deleteAll(noLongerFreeVideos);
                        count += noLongerFreeVideos.size();
                    }
                }
            } catch (HttpClientErrorException e) {
                LOGGER.error("HttpClientError while fetching YouTube videos from channel {}: {}", channelId, e.getMessage());
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    try {
                        processYouTubeVideos(youtubeRepository.findAll(Set.of(channelId)));
                    } catch (MalformedURLException ex) {
                        LOGGER.error("Error", ex);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Error while fetching YouTube videos from channel " + channelId, e);
            }
        }
        syncYouTubeVideos();
        endUpdateMedia(count, start);
    }

    private static List<YouTubeVideo> buildYouTubeVideoList(SearchListResponse searchList, VideoListResponse videoList) {
        return searchList.getItems().stream()
                .map(sr -> toYouTubeVideo(
                        videoList.getItems().stream().filter(v -> sr.getId().getVideoId().equals(v.getId())).findFirst().get()))
                .collect(Collectors.toList());
    }

    private static YouTubeVideo toYouTubeVideo(Video ytVideo) {
        YouTubeVideo video = new YouTubeVideo();
        video.setId(ytVideo.getId());
        video.getMetadata().setAssetUrl(newURL("https://www.youtube.com/watch?v=" + video.getId()));
        VideoSnippet snippet = ytVideo.getSnippet();
        ofNullable(snippet.getChannelId()).ifPresent(video::setChannelId);
        ofNullable(snippet.getChannelTitle()).ifPresent(video::setChannelTitle);
        ofNullable(snippet.getPublishedAt()).map(DateTime::toStringRfc3339).map(Instant::parse).ifPresent(video::setDate);
        ofNullable(snippet.getDescription()).ifPresent(video::setDescription);
        ofNullable(getBestThumbnailUrl(snippet.getThumbnails())).ifPresent(video::setThumbnailUrl);
        ofNullable(snippet.getTitle()).ifPresent(video::setTitle);
        VideoContentDetails details = ytVideo.getContentDetails();
        ofNullable(details.getDuration()).map(Duration::parse).ifPresent(video::setDuration);
        ofNullable(details.getCaption()).map(Boolean::valueOf).ifPresent(video::setCaption);
        return video;
    }

    private static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static URL getBestThumbnailUrl(ThumbnailDetails td) {
        return td != null
                ? newURL(ObjectUtils.firstNonNull(td.getMaxres(), td.getHigh(), td.getMedium(), td.getDefault())
                        .getUrl())
                : null;
    }

    private List<YouTubeVideo> processYouTubeVideos(Iterable<YouTubeVideo> videos) throws MalformedURLException {
        List<YouTubeVideo> result = new ArrayList<>();
        for (YouTubeVideo video : videos) {
            try {
                result.add(processYouTubeVideo(video));
            } catch (IOException e) {
                problem(getSourceUrl(video), e);
            }
        }
        return result;
    }

    private YouTubeVideo processYouTubeVideo(YouTubeVideo video) throws IOException {
        boolean save = false;
        Optional<YouTubeVideo> optVideoInRepo = youtubeRepository.findById(video.getId());
        if (optVideoInRepo.isPresent()) {
            video = optVideoInRepo.get();
        } else {
            save = true;
        }
        Path path = video.getMetadata().getSha1() == null ? downloadVideo(video) : null;
        if (mediaService.updateMedia(video, getOriginalRepository(), path)) {
            save = true;
        }
        if (path != null) {
            Files.deleteIfExists(path);
        }
        if (customProcessing(video)) {
            save = true;
        }
        return save ? youtubeRepository.save(video) : video;
    }

    private Path downloadVideo(YouTubeVideo video) {
        try {
            String url = video.getMetadata().getAssetUrl().toExternalForm();
            String[] output = Utils.execOutput(List.of(
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
    protected final void doUpload(YouTubeVideo video) throws IOException {
        throw new UnsupportedOperationException("<h2>Spacemedia is not able to upload YouTube videos by itself.</h2>\n"
                + "<p>Please go to <a href=\"https://tools.wmflabs.org/video2commons\">video2commons</a> and upload the <b>"
                + video.getId()
                + ".mkv/mp4/webm</b> file, using following information:</p>\n"
                + "<h4>Title:</h4>\n"
                + commonsService.normalizeFilename(video.getUploadTitle())
                + "\n<h4>Wikicode:</h4>\n<pre>" + getWikiCode(video, video.getMetadata()) + "</pre>");
    }

    protected boolean customProcessing(YouTubeVideo video) {
        return false;
    }

    private void syncYouTubeVideos() {
        List<String> categories = new ArrayList<>();
        categories.add("Spacemedia files (review needed)");
        categories.addAll(getAgencyCategories());
        categories.add("Media from YouTube");
        mediaService.syncYouTubeVideos(
                youtubeRepository.findMissingInCommons(youtubeChannels), categories);
    }

    protected abstract List<String> getAgencyCategories();

    @Override
    public Set<String> findCategories(YouTubeVideo media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            // To import by hand from personal account
            result.remove("Spacemedia files uploaded by " + commonsService.getAccount());
        }
        return result;
    }

    @Override
    public Set<String> findTemplates(YouTubeVideo video) {
        Set<String> result = super.findTemplates(video);
        result.add("From YouTube |1= " + video.getId());
        result.add("YouTube CC-BY |1= " + video.getChannelTitle());
        result.add("LicenseReview");
        return result;
    }
}
