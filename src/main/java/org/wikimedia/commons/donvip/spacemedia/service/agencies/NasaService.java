package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.Statistics;
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
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.utils.Geo;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class NasaService extends SpaceAgencyService<NasaMedia, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaService.class);

    @Value("${nasa.search.link}")
    private String searchEndpoint;

    @Value("${nasa.min.year}")
    private int minYear;

    @Value("${nasa.max.tries}")
    private int maxTries;

    @Value("${nasa.centers}")
    private Set<String> nasaCenters;

    @Autowired
    private NasaAudioRepository audioRepository;

    @Autowired
    private NasaImageRepository imageRepository;

    @Autowired
    private NasaVideoRepository videoRepository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private NasaMediaRepository<NasaMedia> mediaRepository;

    @Autowired
    public NasaService(NasaMediaRepository<NasaMedia> repository, ProblemRepository problemrepository) {
        super(repository, problemrepository);
    }

    private NasaMedia save(NasaMedia media) {
        switch (media.getMediaType()) {
        case image: return imageRepository.save((NasaImage) media);
        case video: return videoRepository.save((NasaVideo) media);
        case audio: return audioRepository.save((NasaAudio) media);
        }
        throw new IllegalArgumentException(media.toString());
    }

    static Optional<URL> findOriginalMedia(RestTemplate rest, URL href) throws URISyntaxException {
        return rest.getForObject(Utils.urlToUri(href), NasaAssets.class).stream()
                .filter(u -> u.toExternalForm().contains("~orig.")).findFirst();
    }

    private NasaMedia processMedia(RestTemplate rest, NasaMedia media, URL href) throws IOException, URISyntaxException {
        Optional<NasaMedia> mediaInRepo = repository.findById(media.getNasaId());
        boolean save = false;
        if (mediaInRepo.isPresent()) {
            // allow to purge keywords table and recreate contents
            Set<String> keywordsInRepo = mediaInRepo.get().getKeywords();
            Set<String> keywordsFromNasa = media.getKeywords();
            media = mediaInRepo.get();
            if (CollectionUtils.isEmpty(keywordsInRepo) && !CollectionUtils.isEmpty(keywordsFromNasa)) {
                media.setKeywords(keywordsFromNasa);
                save = true;
            }
        } else {
            save = true;
        }
        // The API is supposed to send us keywords in a proper JSON array, but sometimes it is not
        Set<String> normalizedKeywords = normalizeKeywords(media.getKeywords());
        if (!Objects.equals(normalizedKeywords, media.getKeywords())) {
            media.setKeywords(normalizedKeywords);
            save = true;
        }
        if (media.getAssetUrl() == null) {
            Optional<URL> originalUrl = findOriginalMedia(rest, href);
            if (originalUrl.isPresent()) {
                media.setAssetUrl(originalUrl.get());
                save = true;
            }
        }
        if (mediaService.computeSha1(media, media.getAssetUrl())) {
            save = true;
        }
        if (mediaService.findCommonsFilesWithSha1(media)) {
            save = true;
        }
        if (save) {
            media = save(media);
        }
        if (!nasaCenters.contains(media.getCenter())) {
            problem(media.getAssetUrl(), "Unknown center for id '" + media.getNasaId() + "': " + media.getCenter());
        }
        if (media.getNasaId().length() < 3) {
            problem(media.getAssetUrl(), "Strange id: '" + media.getNasaId() + "'");
        }
        return media;
    }

    static Set<String> normalizeKeywords(Set<String> keywords) {
        if (keywords != null && keywords.size() == 1) {
            String kw = keywords.iterator().next();
            for (String sep : Arrays.asList(",", ";")) {
                if (kw.contains(sep) && looksLikeMultipleValues(kw, sep)) {
                    return Arrays.stream(kw.split(sep)).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
                }
            }
        }
        return keywords;
    }

    private static final Pattern PATTERN_NUMBER = Pattern.compile(".*\\d+,\\d+.*");
    private static final Pattern PATTERN_DATE = Pattern.compile(".*\\p{Alpha}+\\.? \\d{1,2}, [12]\\d{3}.*");
    private static final Pattern PATTERN_A_AND_B = Pattern.compile(".*[\\p{Alpha}\\.]+, (and|&) \\p{Alpha}+.*");
    private static final Pattern PATTERN_A_B_AND_C = Pattern.compile(".*\\p{Alpha}+, \\p{Alpha}+ (and|&) \\p{Alpha}+.*");
    private static final Pattern PATTERN_ER = Pattern.compile("[^,]*\\p{Alpha}+er, \\p{Alpha}+er[^,]*");

    private static boolean looksLikeMultipleValues(String kw, String sep) {
        if (",".equals(sep)) {
            if (kw.startsWith("Hi, ") || kw.contains(", by ")) {
                return false;
            }
            if (kw.endsWith(sep)) {
                kw = kw.substring(0, kw.length()-sep.length());
            }
            if (kw.contains(sep)) {
                String after = kw.substring(kw.lastIndexOf(sep) + sep.length() + " ".length());
                for (Collection<String> entities : Arrays.asList(
                        Geo.CONTINENTS, Geo.COUNTRIES, Geo.STATES, Geo.STATE_CODES, Geo.NORTH_SOUTH_STATES)) {
                    if (entities.contains(after)) {
                        return false;
                    }
                }
                for (Pattern pattern : Arrays.asList(
                        PATTERN_NUMBER, PATTERN_DATE, PATTERN_A_AND_B, PATTERN_A_B_AND_C, PATTERN_ER)) {
                    if (pattern.matcher(kw).matches()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private <T extends NasaMedia> String processSearchResults(RestTemplate rest, String searchUrl, List<T> medias) {
        LOGGER.debug("Fetching {}", searchUrl);
        boolean ok = false;
        for (int i = 0; i < maxTries && !ok; i++) {
            try {
                NasaCollection collection = rest.getForObject(searchUrl, NasaResponse.class).getCollection();
                ok = true;
                for (NasaItem item : collection.getItems()) {
                    try {
                        medias.add((T) processMedia(rest, item.getData().get(0), item.getHref()));
                    } catch (Forbidden e) {
                        problem(item.getHref(), e.getMessage());
                    } catch (RestClientException e) {
                        if (e.getCause() instanceof HttpMessageNotReadableException) {
                            problem(item.getHref(), e.getCause().getMessage());
                        } else {
                            LOGGER.error("Cannot process item " + item, e);
                        }
                    } catch (IOException | URISyntaxException e) {
                        LOGGER.error("Cannot process item " + item, e);
                    }
                }
                if (!CollectionUtils.isEmpty(collection.getLinks())) {
                    Optional<NasaLink> next = collection.getLinks().stream().filter(l -> "next".equals(l.getRel())).findFirst();
                    if (next.isPresent()) {
                        return next.get().getHref().toExternalForm();
                    }
                }
            } catch (RestClientException e) {
                LOGGER.error("Unable to process search results for " + searchUrl, e);
            }
        }
        return null;
    }

    private <T extends NasaMedia> List<T> doUpdateMedia(NasaMediaType mediaType) {
        return doUpdateMedia(mediaType, minYear, LocalDateTime.now().getYear(), null);
    }

    private <T extends NasaMedia> List<T> doUpdateMedia(NasaMediaType mediaType, int year, Set<String> centers) {
        return doUpdateMedia(mediaType, year, year, centers);
    }

    private <T extends NasaMedia> List<T> doUpdateMedia(NasaMediaType mediaType, int startYear, int endYear,
            Set<String> centers) {
        LocalDateTime start = LocalDateTime.now();
        logStartUpdate(mediaType, startYear, endYear, centers);
        final List<T> medias = new ArrayList<>();
        RestTemplate rest = new RestTemplate();
        String nextUrl = searchEndpoint + "media_type=" + mediaType + "&year_start=" + startYear + "&year_end=" + endYear;
        if (centers != null) {
            nextUrl += "&center=" + String.join(",", centers);
        }
        while (nextUrl != null) {
            nextUrl = processSearchResults(rest, nextUrl, medias);
        }
        logEndUpdate(mediaType, startYear, endYear, centers, start, medias.size());
        return medias;
    }

    private static void logStartUpdate(NasaMediaType mediaType, int startYear, int endYear, Set<String> centers) {
        if (centers == null) {
            if (startYear == endYear) {
                LOGGER.info("NASA {} update for year {} started...", mediaType, startYear);
            } else {
                LOGGER.info("NASA {} update for years {}-{} started...", mediaType, startYear, endYear);
            }
        } else if (startYear == endYear && centers.size() == 1) {
            LOGGER.info("NASA {} update for year {} center {} started...", mediaType, startYear,
                    centers.iterator().next());
        } else {
            LOGGER.info("NASA {} update for years {}-{} center {} started...", mediaType, startYear, endYear, centers);
        }
    }

    private static void logEndUpdate(NasaMediaType mediaType, int startYear, int endYear, Set<String> centers, LocalDateTime start, int size) {
        Duration duration = Duration.between(LocalDateTime.now(), start);
        if (centers == null) {
            if (startYear == endYear) {
                LOGGER.info("NASA {} update for year {} completed: {} {}s in {}",
                        mediaType, startYear, size, mediaType, duration);
            } else {
                LOGGER.info("NASA {} update for years {}-{} completed: {} {}s in {}",
                        mediaType, startYear, endYear, size, mediaType, duration);
            }
        } else if (startYear == endYear && centers.size() == 1) {
            LOGGER.info("NASA {} update for year {} center {} completed: {} {}s in {}",
                    mediaType, startYear, centers.iterator().next(), size, mediaType, duration);
        } else {
            LOGGER.info("NASA {} update for years {}-{} center {} completed: {} {}s in {}",
                    mediaType, startYear, endYear, centers.iterator().next(), size, mediaType, duration);
        }
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}", initialDelayString = "${initial.delay}")
    public List<NasaImage> updateImages() {
        List<NasaImage> images = new ArrayList<>();
        // Recent years have a lot of photos: search by center to avoid more than 10k results
        for (int year = LocalDateTime.now().getYear(); year >= 2000; year--) {
            for (String center : nasaCenters) {
                images.addAll(doUpdateMedia(NasaMediaType.image, year, Collections.singleton(center)));
            }
        }
        // Ancient years have a lot less photos: simple search for all centers
        for (int year = 1999; year >= minYear; year--) {
            images.addAll(doUpdateMedia(NasaMediaType.image, year, null));
        }
        return images;
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}", initialDelayString = "${initial.delay}")
    public List<NasaAudio> updateAudios() {
        return doUpdateMedia(NasaMediaType.audio);
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}", initialDelayString = "${initial.delay}")
    public List<NasaVideo> updateVideos() {
        return doUpdateMedia(NasaMediaType.video);
    }

    @Override
    public List<NasaMedia> updateMedia() {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting NASA medias update...");
        final List<NasaMedia> medias = new ArrayList<>();
        medias.addAll(updateImages());
        medias.addAll(updateAudios());
        medias.addAll(updateVideos());
        LOGGER.info("NASA medias update completed: {} medias in {}", medias.size(), Duration.between(LocalDateTime.now(), start));
        return medias;
    }

    @Override
    public String getName() {
        return "NASA";
    }

    @Override
    public Statistics getStatistics() {
        Statistics stats = super.getStatistics();
        List<String> centers = mediaRepository.listCenters();
        if (centers.size() > 1) {
            stats.setDetails(centers.parallelStream()
                    .map(c -> new Statistics(Objects.toString(c), mediaRepository.countByCenter(c),
                            mediaRepository.countMissingInCommonsByCenter(c), null))
                    .sorted().collect(Collectors.toList()));
        }
        return stats;
    }
}
