package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;

@Service
public class ArianespaceYouTubeService extends AbstractAgencyYouTubeService {

    public ArianespaceYouTubeService(
            YouTubeVideoRepository repository,
            @Value("${arianespace.youtube.channels}") Set<String> youtubeChannels) {
        super(repository, youtubeChannels);
    }

    @Override
    public String getName() {
        return "Arianespace (YouTube)";
    }

    @Override
    @Scheduled(fixedRateString = "${arianespace.update.rate}", initialDelayString = "${arianespace.initial.delay}")
    public void updateMedia() {
        updateYouTubeVideos();
    }

    @Override
    protected String getAuthor(YouTubeVideo media) throws MalformedURLException {
        return "Arianespace";
    }

    @Override
    protected boolean applyIgnoreRules(YouTubeVideo video) {
        boolean result = super.applyIgnoreRules(video);
        if (!Boolean.TRUE.equals(video.isIgnored()) && video.getDescription().contains("copyright: ROSCOSMOS")) {
            video.setIgnored(Boolean.TRUE);
            video.setIgnoredReason("ROSCOSMOS copyright");
            result = true;
        }
        if (!Boolean.TRUE.equals(video.isIgnored()) && video.getDuration().compareTo(Duration.ofMinutes(6)) > 0) {
            video.setIgnored(Boolean.TRUE);
            video.setIgnoredReason("Video longer than 6 minutes");
            result = true;
        }
        return result;
    }

    @Override
    public Set<String> findCategories(YouTubeVideo video, boolean includeHidden) {
        Set<String> result = super.findCategories(video, includeHidden);
        if (includeHidden) {
            if (video.getTitle().contains("VA")) {
                result.add("Ariane 5 Launch Videos by Arianespace");
            } else if (video.getTitle().contains("VV")) {
                result.add("Vega Launch Videos by Arianespace");
            } else {
                result.add("Videos by Arianespace");
            }
        }
        return result;
    }
}
