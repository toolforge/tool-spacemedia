package org.wikimedia.commons.donvip.spacemedia.repo.youtube;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvesterService;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Organization;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

@Service
public class YouTubeHarvesterService extends AbstractHarvesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeHarvesterService.class);

    private static final String YOUTUBE_CONTEXT = "YouTube";

    @Value("${youtube.depot.id.prefix}")
    private String depotIdPrefix;

    @Value("${youtube.depot.name.prefix}")
    private String depotNamePrefix;

    @Value("${youtube.org.id}")
    private String orgId;

    @Value("${youtube.channels}")
    private Set<String> youtubeChannels;

    @Value("${youtube.denied.terms}")
    private Set<String> deniedTerms;

    @Value("${youtube.sentences.to.remove}")
    private Set<String> sentencestoRemove;

    @Value("${youtube.max.duration}")
    private Duration maxDuration;

    @Value("${youtube.duplicatedIds}")
    private Set<String> duplicatedIds;

    @Autowired
    private YouTubeApiService youtubeService;

    @Override
    public void harvestMedia() throws IOException {
        LocalDateTime start = startUpdateMedia(depotIdPrefix);
        int count = 0;
        for (String channelId : youtubeChannels) {
            try {
                Organization org = organizationRepository.findById(orgId).orElseThrow();
                Depot depot = depotRepository.findOrCreate(depotIdPrefix + '-' + channelId,
                        depotNamePrefix + ' ' + channelId, new URL("https://www.youtube.com/channel/" + channelId),
                        Set.of(org, organizationRepository.findById("YouTube").orElseThrow()));
                LOGGER.info("Fetching YouTube videos from channel '{}'...", channelId);
                List<MediaPublication> freeVideos = new ArrayList<>();
                String pageToken = null;
                do {
                    SearchListResponse list = youtubeService.searchVideos(channelId, pageToken);
                    pageToken = list.getNextPageToken();
                    List<MediaPublication> videos = processYouTubeVideos(
                            buildYouTubeVideoList(depot, org, list, youtubeService.listVideos(list)));
                    count += videos.size();
                    freeVideos.addAll(videos);
                } while (pageToken != null);
                if (!freeVideos.isEmpty()) {
                    Set<MediaPublication> noLongerFreeVideos = mediaPublicationRepository
                            .findByDepotIdIn(Set.of(channelId));
                    noLongerFreeVideos.removeAll(freeVideos);
                    if (!noLongerFreeVideos.isEmpty()) {
                        LOGGER.warn("Deleting {} videos no longer-free for channel {}: {}", noLongerFreeVideos.size(),
                                channelId, noLongerFreeVideos);
                        mediaPublicationRepository.deleteAll(noLongerFreeVideos);
                        count += noLongerFreeVideos.size();
                    }
                }
            } catch (HttpClientErrorException e) {
                LOGGER.error("HttpClientError while fetching YouTube videos from channel {}: {}", channelId,
                        e.getMessage());
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    try {
                        processYouTubeVideos(mediaPublicationRepository.findByDepotIdIn(Set.of(channelId)));
                    } catch (MalformedURLException ex) {
                        LOGGER.error("Error", ex);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Error while fetching YouTube videos from channel " + channelId, e);
            }
        }
        endUpdateMedia(depotIdPrefix, count, start);
    }

    private List<MediaPublication> buildYouTubeVideoList(Depot depot, Organization org, SearchListResponse searchList,
            VideoListResponse videoList) {
        return searchList.getItems().stream()
                .map(sr -> toYouTubeVideo(depot, org, videoList.getItems().stream()
                        .filter(v -> sr.getId().getVideoId().equals(v.getId())).findFirst().get()))
                .collect(Collectors.toList());
    }

    private MediaPublication toYouTubeVideo(Depot depot, Organization org, Video ytVideo) {
        MediaPublication video = new MediaPublication();
        video.setId(new PublicationKey(depot.getId(), ytVideo.getId()));
        video.setLicence(Licence.CC_BY_3_0);
        video.setDepot(depot);
        video.addAuthor(org);
        video.setUrl(newURL("https://www.youtube.com/watch?v=" + video.getId().getId()));
        VideoSnippet snippet = ytVideo.getSnippet();
        ofNullable(snippet.getDefaultLanguage()).or(() -> ofNullable(snippet.getDefaultAudioLanguage()))
                .ifPresent(video::setLang);
        ofNullable(snippet.getPublishedAt()).map(x -> Instant.parse(x.toStringRfc3339()).atZone(ZoneOffset.UTC))
                .ifPresent(video::setPublicationDateTime);
        ofNullable(snippet.getDescription()).ifPresent(video::setDescription);
        ofNullable(getBestThumbnailUrl(snippet.getThumbnails())).ifPresent(video::setThumbnailUrl);
        ofNullable(snippet.getTitle()).ifPresent(video::setTitle);
        VideoContentDetails details = ytVideo.getContentDetails();
        ofNullable(details.getDuration()).map(Duration::parse)
                .ifPresent(x -> addYouTubeMetadata(video, YouTubeMetadata.DURATION, x));
        ofNullable(details.getCaption()).map(Boolean::valueOf)
                .ifPresent(x -> addYouTubeMetadata(video, YouTubeMetadata.CAPTION, x));
        return video;
    }

    private boolean addYouTubeMetadata(MediaPublication video, YouTubeMetadata key, Object value) {
        return video.addMetadata(metadataRepository.findOrCreate(YOUTUBE_CONTEXT, key.name(), value.toString()));
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

    private List<MediaPublication> processYouTubeVideos(Iterable<MediaPublication> videos)
            throws MalformedURLException {
        List<MediaPublication> result = new ArrayList<>();
        for (MediaPublication video : videos) {
            try {
                result.add(processYouTubeVideo(video));
            } catch (IOException e) {
                problem(video.getUrl(), e);
            }
        }
        return result;
    }

    private MediaPublication processYouTubeVideo(MediaPublication video) throws IOException {
        boolean save = false;
        Optional<MediaPublication> videoInRepo = mediaPublicationRepository.findById(video.getId());
        if (videoInRepo.isPresent()) {
            video = videoInRepo.get();
        } else {
            save = true;
        }
        if (customProcessing(video)) {
            save = true;
        }
        return save ? mediaPublicationRepository.save(video) : video;
    }

    protected boolean customProcessing(MediaPublication video) {
        boolean result = false;
        for (String deniedTerm : deniedTerms) {
            if (video.getDescription().contains(deniedTerm)) {
                ignoreFile(video, "Denied term: " + deniedTerm);
                result = true;
            }
        }
        Set<String> durations = video.getMetadataValues(YouTubeMetadata.DURATION.name());
        if (!durations.isEmpty() && Duration.parse(durations.iterator().next()).compareTo(maxDuration) > 0) {
            ignoreFile(video, "Video longer than " + maxDuration);
            result = true;
        }
        for (String duplicatedId : duplicatedIds) {
            if (duplicatedId.equals(video.getId().getId())) {
                ignoreFile(video, "Video duplicated: " + duplicatedId);
                result = true;
            }
        }
        for (String toRemove : sentencestoRemove) {
            if (video.getDescription().contains(toRemove)) {
                video.setDescription(video.getDescription().replace(toRemove, "").trim());
                result = true;
            }
        }
        return result;
    }
}
