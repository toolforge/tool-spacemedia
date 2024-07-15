package org.wikimedia.commons.donvip.spacemedia.service.youtube;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkId;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.youtube.YouTubeMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Lazy
@Service
public class YouTubeMediaProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeMediaProcessor.class);

    // Taken from
    // https://github.com/eatcha-wikimedia/YouTubeReviewBot/blob/master/main.py
    private static final Pattern FROM_YOUTUBE = Pattern.compile(
            ".*\\{\\{\\s*?[Ff]rom\\s[Yy]ou[Tt]ube\\s*(?:\\||\\|1\\=|\\s*?)(?:\\s*)(?:1|=\\||)(?:=|)([^\"&?\\/ \\}]{11}).*",
            Pattern.DOTALL);

    // Taken from
    // https://github.com/eatcha-wikimedia/YouTubeReviewBot/blob/master/main.py
    private static final Pattern YOUTUBE_URL = Pattern.compile(
            ".*https?\\:\\/\\/(?:www|m|)(?:|\\.)youtube\\.com/watch\\W(?:feature\\=player_embedded&)?v\\=([^\"&?\\/ \\}]{11}).*",
            Pattern.DOTALL);

    @Autowired
    private YouTubeMediaRepository youtubeRepository;

    @Autowired
    private CommonsService commonsService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private YouTubeMediaProcessor self;

    private static String findYouTubeId(String text) {
        if (StringUtils.isNotBlank(text)) {
            Matcher m = FROM_YOUTUBE.matcher(text);
            if (m.matches()) {
                return m.group(1).strip();
            } else {
                m = YOUTUBE_URL.matcher(text);
                if (m.matches()) {
                    return m.group(1).strip();
                }
            }
        }
        return null;
    }

    public void syncYouTubeVideos(List<YouTubeMedia> videos, List<String> categories) {
        for (String category : categories) {
            if (isEmpty(self.syncYouTubeVideos(videos, category))) {
                break;
            }
        }
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    public List<YouTubeMedia> syncYouTubeVideos(List<YouTubeMedia> missingVideos, String category) {
        if (isNotEmpty(missingVideos)) {
            LOGGER.info("Starting YouTube videos synchronization from {}...", category);
            LocalDateTime start = LocalDateTime.now();
            Pageable pageRequest = PageRequest.of(0, 500);
            Page<CommonsCategoryLinkId> page = null;
            do {
                page = commonsService.getFilesInCategory(category, pageRequest);
                for (CommonsCategoryLinkId link : page) {
                    try {
                        CommonsPage from = link.getFrom();
                        String title = from.getTitle();
                        if (title.endsWith(".ogv") || title.endsWith(".webm")) {
                            String id = findYouTubeId(commonsService.getPageContent(from));
                            if (StringUtils.isNotBlank(id)) {
                                Optional<YouTubeMedia> opt = missingVideos.stream()
                                        .filter(v -> v.getId().getMediaId().equals(id))
                                        .findFirst();
                                if (opt.isPresent()) {
                                    missingVideos.remove(self.updateYouTubeCommonsFileName(opt.get(), title));
                                    if (missingVideos.isEmpty()) {
                                        break;
                                    }
                                }
                            } else {
                                LOGGER.warn("Cannot find YouTube video identifier for: {}", title);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Failed to get page content of {}", link, e);
                        GlitchTip.capture(e);
                    }
                }
                pageRequest = page.nextPageable();
            } while (page.hasNext() && !missingVideos.isEmpty());
            LOGGER.info("YouTube videos synchronization from {} completed in {}", category, Utils.durationInSec(start));
        }
        return missingVideos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public YouTubeMedia updateYouTubeCommonsFileName(YouTubeMedia video, String filename) {
        mediaService.saveNewMetadataCommonsFileNames(video.getUniqueMetadata(), new HashSet<>(Set.of(filename)));
        return youtubeRepository.save(video);
    }
}
