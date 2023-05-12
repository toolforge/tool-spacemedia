package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class ArianespaceYouTubeService extends AbstractAgencyYouTubeService {

    private static final List<String> TO_REMOVE = List.of("Category Science & Technology",
            "Licence Creative Commons Attribution licence (reuse allowed)",
            "Follow the launch live at http://www.arianespace.com and on http://www.youtube.com/arianespace",
            "Since its creation in 1980 as the world's first commercial space transportation company, Arianespace has led the launch services industry with many operational firsts and numerous record-setting missions.");

    public ArianespaceYouTubeService(
            YouTubeVideoRepository repository,
            @Value("${arianespace.youtube.channels}") Set<String> youtubeChannels) {
        super(repository, "arianespace", youtubeChannels);
    }

    @Override
    public String getName() {
        return "Arianespace (YouTube)";
    }

    @Override
    public void updateMedia() {
        updateYouTubeVideos();
    }

    @Override
    protected String getAuthor(YouTubeVideo media) throws MalformedURLException {
        return "Arianespace";
    }

    @Override
    protected boolean customProcessing(YouTubeVideo video) {
        boolean result = super.customProcessing(video);
        if (!Boolean.TRUE.equals(video.isIgnored()) && video.getDescription().contains("copyright: ROSCOSMOS")) {
            result = ignoreFile(video, "ROSCOSMOS copyright");
        }
        if (!Boolean.TRUE.equals(video.isIgnored()) && video.getDuration().compareTo(Duration.ofMinutes(6)) > 0) {
            result = ignoreFile(video, "Video longer than 6 minutes");
        }
        if ("yMy9IfNqJ2k".equals(video.getId())) {
            result = ignoreFile(video, "Video duplicated");
        }
        for (String toRemove : TO_REMOVE) {
            if (video.getDescription().contains(toRemove)) {
                video.setDescription(video.getDescription().replace(toRemove, "").trim());
                result = true;
            }
        }
        return result;
    }

    @Override
    public Set<String> findCategories(YouTubeVideo video, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(video, metadata, includeHidden);
        if (includeHidden) {
            if (video.getTitle().contains("VA")) {
                result.add("Videos of Ariane 5 launches by Arianespace");
            } else if (video.getTitle().contains("VV")) {
                result.add("Videos of Vega launches by Arianespace");
            } else if (video.getTitle().contains("VS") || video.getTitle().contains("ST")) {
                result.add("Videos of Soyouz launches by Arianespace");
            } else {
                result.add("Videos by Arianespace");
            }
        }
        return result;
    }

    @Override
    protected List<String> getAgencyCategories() {
        return List.of("Videos by Arianespace",
                "Videos of Ariane launches by Arianespace",
                "Videos of Ariane 5 launches by Arianespace",
                "Videos of Soyouz launches by Arianespace",
                "Videos of Vega launches by Arianespace");
    }

    @Override
    protected Set<String> getEmojis(YouTubeVideo uploadedMedia) {
        return Set.of(Emojis.ROCKET);
    }

    @Override
    protected Set<String> getTwitterAccounts(YouTubeVideo uploadedMedia) {
        return Set.of("@Arianespace");
    }
}
