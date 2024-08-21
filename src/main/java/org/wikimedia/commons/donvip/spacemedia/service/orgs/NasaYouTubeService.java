package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaYouTubeService extends AbstractOrgYouTubeService {

    public NasaYouTubeService(YouTubeMediaRepository repository,
            @Value("${nasa.youtube.channels}") Set<String> youtubeChannels) {
        super(repository, "nasa.youtube", youtubeChannels);
    }

    @Override
    public String getName() {
        return "NASA (YouTube)";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia NASA YouTube files uploaded by " + commonsService.getAccount();
    }

    @Override
    public String getUiRepoId(String repoId) {
        // Remove "nasa" from ids displayed in UI to make the long list fit in screen
        return super.getUiRepoId(repoId).replaceAll("(?i)nasa[_-]?", "").replace("Video", "");
    }

    @Override
    protected String getAuthor(YouTubeMedia media, FileMetadata metadata) {
        return "NASA";
    }

    @Override
    protected List<String> getOrgCategories() {
        return List.of("Videos of NASA");
    }

    @Override
    protected Set<String> getEmojis(YouTubeMedia uploadedMedia) {
        return Set.of(Emojis.ROCKET);
    }
}
