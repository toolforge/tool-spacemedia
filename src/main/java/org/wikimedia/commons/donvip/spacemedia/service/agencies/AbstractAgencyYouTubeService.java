package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    protected YouTubeVideoRepository youtubeRepository;
    @Autowired
    private YouTubeService youtubeService;

    protected final Set<String> youtubeChannels;

    public AbstractAgencyYouTubeService(YouTubeVideoRepository repository, Set<String> youtubeChannels) {
        super(repository);
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

    protected void updateYouTubeVideos() {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        for (String channelId : youtubeChannels) {
            try {
                LOGGER.info("Fetching YouTube videos from channel '{}'...", channelId);
                String pageToken = null;
                do {
                    SearchListResponse list = youtubeService.searchVideos(channelId, pageToken);
                    pageToken = list.getNextPageToken();
                    count += processYouTubeVideos(buildYouTubeVideoList(list, youtubeService.listVideos(list)));
                } while (pageToken != null);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Error while fetching YouTube videos from channel " + channelId, e);
            }
        }
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

    private int processYouTubeVideos(List<YouTubeVideo> videos) throws MalformedURLException {
        int count = 0;
        for (YouTubeVideo video : videos) {
            try {
                processYouTubeVideo(video);
                count++;
            } catch (IOException e) {
                problem(getSourceUrl(video), e);
            }
        }
        return count;
    }

    private void processYouTubeVideo(YouTubeVideo video) throws IOException {
        boolean save = false;
        Optional<YouTubeVideo> optVideoInRepo = youtubeRepository.findById(video.getId());
        if (optVideoInRepo.isPresent()) {
            video = optVideoInRepo.get();
        } else {
            save = true;
        }
        if (mediaService.updateMedia(video, getOriginalRepository(),
                video.getMetadata().getSha1() == null ? downloadVideo(video) : null)) {
            save = true;
        }
        if (customProcessing(video)) {
            save = true;
        }
        if (save) {
            youtubeRepository.save(video);
        }
    }

    private Path downloadVideo(YouTubeVideo video) {
        try {
            String url = video.getMetadata().getAssetUrl().toExternalForm();
            String line = Arrays.stream(Utils
                    .execOutput(List.of("youtube-dl", "--no-progress", "--id", url), 30, TimeUnit.MINUTES).split("\n"))
                    .filter(l -> l.contains("Destination:")).findFirst().get();
            return Paths.get(line.substring(line.indexOf(": ") + ": ".length()));
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.error("Error while downloading YouTube video", e);
        }
        return null;
    }

    @Override
    protected void doUpload(String wikiCode, YouTubeVideo video) throws IOException {
        throw new UnsupportedOperationException("<h2>Spacemedia is not able to upload YouTube videos by itself.</h2>\n"
                + "<p>Please go to <a href=\"https://tools.wmflabs.org/video2commons\">video2commons</a> and upload the <b>"
                + video.getId()
                + ".mp4</b> file, using following information:</p>\n"
                + "<h4>Title:</h4>\n"
                + video.getUploadTitle()
                + "\n<h4>Wikicode:</h4>\n<pre>" + wikiCode + "</pre>");
    }

    protected boolean customProcessing(YouTubeVideo video) {
        return false;
    }

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
    public List<String> findTemplates(YouTubeVideo video) {
        List<String> result = super.findTemplates(video);
        result.add("From YouTube |1= " + video.getId());
        result.add("YouTube CC-BY |1= " + video.getChannelTitle());
        result.add("Licensereview");
        return result;
    }
}
