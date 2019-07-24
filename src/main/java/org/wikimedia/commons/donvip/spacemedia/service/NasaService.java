package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaAssets;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaAudio;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaCollection;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaImage;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaItem;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaLink;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaResponse;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaVideo;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class NasaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaService.class);

    @Value("${nasa.search.link}")
    private String searchEndpoint;
    
    @Autowired
    private NasaAudioRepository audioRepository;

    @Autowired
    private NasaImageRepository imageRepository;

    @Autowired
    private NasaVideoRepository videoRepository;

    @Autowired
    @Qualifier("nasaMediaRepository")
    private NasaMediaRepository<?> mediaRepository;

    @Autowired
    private CommonsService commonsService;

    public Iterable<? extends NasaMedia> listAllMedia() throws IOException {
        return mediaRepository.findAll();
    }

    public List<NasaMedia> listMissingMedia() throws IOException {
        return mediaRepository.findMissingInCommons();
    }

    public List<NasaMedia> listDuplicateMedia() throws IOException {
        return mediaRepository.findDuplicateInCommons();
    }

    private NasaMedia save(NasaMedia media) {
        switch (media.getMediaType()) {
        case image: return imageRepository.save((NasaImage) media);
        case video: return videoRepository.save((NasaVideo) media);
        case audio: return audioRepository.save((NasaAudio) media);
        }
        throw new IllegalArgumentException(media.toString());
    }

    private NasaMedia processMedia(RestTemplate rest, NasaMedia media, URL href) throws IOException, URISyntaxException {
        Optional<? extends NasaMedia> mediaInRepo = mediaRepository.findById(media.getNasaId());
        boolean save = false;
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            save = true;
        }
        if (media.getAssetUrl() == null) {
            Optional<URL> originalUrl = rest.getForObject(href.toExternalForm(), NasaAssets.class).stream()
                    .filter(u -> u.toExternalForm().contains("~orig.")).findFirst();
            if (originalUrl.isPresent()) {
                media.setAssetUrl(originalUrl.get());
                save = true;
            }
        }
        if (media.getAssetUrl() != null && media.getSha1() == null) {
            media.setSha1(Utils.computeSha1(media.getAssetUrl()));
            save = true;
        }
        if (media.getSha1() != null) {
            Set<String> files = commonsService.findFilesWithSha1(media.getSha1());
            if (!files.isEmpty()) {
                media.setCommonsFileNames(files);
                save = true;
            }
        }
        if (save) {
            media = save(media);
        }
        return media;
    }

    private String processSearchResults(RestTemplate rest, String searchUrl) {
        LOGGER.debug("Fetching {}", searchUrl);
        NasaCollection collection = rest.getForObject(searchUrl, NasaResponse.class).getCollection();
        for (NasaItem item : collection.getItems()) {
            try {
                processMedia(rest, item.getData().get(0), item.getHref());
            } catch (IOException | RestClientException | URISyntaxException e) {
                LOGGER.error("Cannot process item " + item, e);
            }
        }
        if (!CollectionUtils.isEmpty(collection.getLinks())) {
            Optional<NasaLink> next = collection.getLinks().stream().filter(l -> "next".equals(l.getRel())).findFirst();
            if (next.isPresent()) {
                return next.get().getHref().toExternalForm();
            }
        }
        return null;
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}")
    public List<NasaMedia> updateMedia() {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting NASA medias update...");
        final List<NasaMedia> medias = new ArrayList<>();
        RestTemplate rest = new RestTemplate();
        for (NasaMediaType mediaType : NasaMediaType.values()) {
            LOGGER.info("Starting NASA {} update...", mediaType);
            String nextUrl = searchEndpoint + "media_type=" + mediaType;
            while (nextUrl != null) {
                nextUrl = processSearchResults(rest, nextUrl);
            }
        }
        LOGGER.info("NASA medias update completed: {} medias in {}", medias.size(), Duration.between(LocalDateTime.now(), start));
        return medias;
    }
}
