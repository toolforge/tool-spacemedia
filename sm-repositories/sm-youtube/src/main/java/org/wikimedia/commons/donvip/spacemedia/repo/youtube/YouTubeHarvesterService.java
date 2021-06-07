package org.wikimedia.commons.donvip.spacemedia.repo.youtube;

import static com.github.kiulian.downloader.model.quality.VideoQuality.hd1080;
import static com.github.kiulian.downloader.model.quality.VideoQuality.hd1440;
import static com.github.kiulian.downloader.model.quality.VideoQuality.hd2160;
import static com.github.kiulian.downloader.model.quality.VideoQuality.hd2880p;
import static com.github.kiulian.downloader.model.quality.VideoQuality.hd720;
import static com.github.kiulian.downloader.model.quality.VideoQuality.highres;
import static com.github.kiulian.downloader.model.quality.VideoQuality.large;
import static com.github.kiulian.downloader.model.quality.VideoQuality.medium;
import static com.github.kiulian.downloader.model.quality.VideoQuality.small;
import static com.github.kiulian.downloader.model.quality.VideoQuality.tiny;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Organization;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.quality.VideoQuality;
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

    @Value("${youtube.licence.text}")
    private String licenceText;

    @Value("${youtube.licence}")
    private Licence licence;

    @Autowired
    private YouTubeApiService youtubeService;

    private IOException error;

    @Override
    public void harvestMedia() throws IOException {
        LocalDateTime start = startUpdateMedia(depotIdPrefix);
        int count = 0;
        Organization org = organizationRepository.findById(orgId).orElseThrow();
        Set<Organization> operators = Set.of(org, organizationRepository.findById("YouTube").orElseThrow());
        for (String channelId : youtubeChannels) {
            try {
                Depot depot = depotRepository.findOrCreate(depotIdPrefix + '-' + channelId,
                        depotNamePrefix + ' ' + channelId, new URL("https://www.youtube.com/channel/" + channelId),
                        operators);
                LOGGER.info("Fetching YouTube videos from channel '{}'...", channelId);
                List<MediaPublication> freeVideos = new ArrayList<>();
                count += performSearch(freeVideos, depot, org,
                        pageToken -> {
                            try {
                                return youtubeService.searchCreativeCommonsVideos(channelId, pageToken);
                            } catch (IOException e) {
                                error = e;
                                return null;
                            }
                        });
                if (licenceText != null) {
                    count += performSearch(freeVideos, depot, org,
                            pageToken -> {
                                try {
                                    return youtubeService.searchVideos(channelId, pageToken, licenceText);
                                } catch (IOException e) {
                                    error = e;
                                    return null;
                                }
                            });
                }
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
                error = null;
            }
        }
        endUpdateMedia(depotIdPrefix, count, start);
    }

    private int performSearch(List<MediaPublication> freeVideos, Depot depot, Organization org,
            Function<String, SearchListResponse> search) throws IOException {
        String pageToken = null;
        int count = 0;
        do {
            SearchListResponse list = search.apply(pageToken);
            if (list != null) {
                pageToken = list.getNextPageToken();
                List<MediaPublication> videos = processYouTubeVideos(
                        buildMediaPublicationList(depot, org, list, youtubeService.listVideos(list)));
                count += videos.size();
                freeVideos.addAll(videos);
            } else {
                pageToken = null;
            }
        } while (pageToken != null);
        if (error != null) {
            throw error;
        }
        return count;
    }

    private List<MediaPublication> buildMediaPublicationList(Depot depot, Organization org,
            SearchListResponse searchList, VideoListResponse videoList) {
        YoutubeDownloader downloader = new YoutubeDownloader();
        return searchList.getItems().stream()
                .map(sr -> toMediaPublication(depot, org, downloader, videoList.getItems().stream()
                        .filter(v -> sr.getId().getVideoId().equals(v.getId())).findFirst().get()))
                .collect(Collectors.toList());
    }

    private MediaPublication toMediaPublication(Depot depot, Organization org, YoutubeDownloader downloader,
            Video ytVideo) {
        String description = ytVideo.getSnippet().getDescription();
        MediaPublication media = new MediaPublication();
        media.setId(new PublicationKey(depot.getId(), ytVideo.getId()));
        ytVideo.getContentDetails().getLicensedContent();
        if (YouTubeApiService.isCreativeCommons(ytVideo)) {
            media.setLicence(Licence.CC_BY_3_0);
        } else if (licenceText != null && licence != null && description != null && description.contains(licenceText)) {
            media.setLicence(licence);
        }
        media.setDepot(depot);
        media.addAuthor(org);
        String id = media.getId().getId();
        media.setUrl(newURL("https://www.youtube.com/watch?v=" + id));
        VideoSnippet snippet = ytVideo.getSnippet();
        ofNullable(snippet.getDefaultLanguage()).or(() -> ofNullable(snippet.getDefaultAudioLanguage()))
                .ifPresent(media::setLang);
        ofNullable(snippet.getPublishedAt()).map(x -> Instant.parse(x.toStringRfc3339()).atZone(ZoneOffset.UTC))
                .ifPresent(media::setPublicationDateTime);
        ofNullable(snippet.getDescription()).ifPresent(media::setDescription);
        ofNullable(getBestThumbnailUrl(snippet.getThumbnails())).ifPresent(media::setThumbnailUrl);
        ofNullable(snippet.getTitle()).ifPresent(media::setTitle);
        VideoContentDetails details = ytVideo.getContentDetails();
        ofNullable(details.getDuration()).map(Duration::parse)
                .ifPresent(x -> addYouTubeMetadata(media, YouTubeMetadata.DURATION, x));
        ofNullable(details.getCaption()).map(Boolean::valueOf)
                .ifPresent(x -> addYouTubeMetadata(media, YouTubeMetadata.CAPTION, x));
        try {
            for (VideoFormat format : findVideosInBestQuality(downloader, id)) {
                FilePublication file = new FilePublication(depot, id, new URL(format.url()));
                file.setCredit(media.getCredit());
                file.setLicence(media.getLicence());
                file.setThumbnailUrl(media.getThumbnailUrl());
                file.setPublicationDateTime(media.getPublicationDateTime());
                media.addFilePublication(filePublicationRepository.save(file));
            }
        } catch (YoutubeException | MalformedURLException e) {
            LOGGER.error("Error while retrieving YouTube download link", e);
        }
        return media;
    }

    private List<VideoFormat> findVideosInBestQuality(YoutubeDownloader downloader, String id) throws YoutubeException {
        YoutubeVideo video = downloader.getVideo(id);
        for (VideoQuality quality : new VideoQuality[] {
                highres, // 3072p
                hd2880p,
                hd2160,
                hd1440,
                hd1080,
                hd720,
                large, // 480p
                medium, // 360p
                small, // 240p
                tiny // 144p
        }) {
            List<VideoFormat> formats = video.findVideoWithQuality(quality);
            if (!formats.isEmpty()) {
                return formats;
            }
        }
        return Collections.emptyList();
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
        if (maxDuration != null && !durations.isEmpty()
                && Duration.parse(durations.iterator().next()).compareTo(maxDuration) > 0) {
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
