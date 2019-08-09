package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Value("${nasa.min.year}")
    private int minYear;

    @Value("${nasa.max.tries}")
    private int maxTries;

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

    static Optional<URL> findOriginalMedia(RestTemplate rest, URL href) throws RestClientException, URISyntaxException {
        return rest.getForObject(Utils.urlToUri(href), NasaAssets.class).stream()
                .filter(u -> u.toExternalForm().contains("~orig.")).findFirst();
    }

    private NasaMedia processMedia(RestTemplate rest, NasaMedia media, URL href) throws IOException, URISyntaxException {
        Optional<? extends NasaMedia> mediaInRepo = mediaRepository.findById(media.getNasaId());
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
    private static final Pattern PATTERN_ER = Pattern.compile(".*\\p{Alpha}+er, \\p{Alpha}+er.*");

    private static final List<String> CONTINENTS = Arrays.asList(
            "Africa", "Antarctica", "Asia", "Australia", "Eurasia", "Europe", "Oceania",
            "America", "Central America", "North America", "South America");

    public static final Map<String, String> STATE_MAP;
    static {
        STATE_MAP = new HashMap<String, String>();
        STATE_MAP.put("AL", "Alabama");
        STATE_MAP.put("AK", "Alaska");
        STATE_MAP.put("AB", "Alberta");
        STATE_MAP.put("AZ", "Arizona");
        STATE_MAP.put("AR", "Arkansas");
        STATE_MAP.put("BC", "British Columbia");
        STATE_MAP.put("CA", "California");
        STATE_MAP.put("CO", "Colorado");
        STATE_MAP.put("CT", "Connecticut");
        STATE_MAP.put("DE", "Delaware");
        STATE_MAP.put("DC", "District Of Columbia");
        STATE_MAP.put("FL", "Florida");
        STATE_MAP.put("GA", "Georgia");
        STATE_MAP.put("GU", "Guam");
        STATE_MAP.put("HI", "Hawaii");
        STATE_MAP.put("ID", "Idaho");
        STATE_MAP.put("IL", "Illinois");
        STATE_MAP.put("IN", "Indiana");
        STATE_MAP.put("IA", "Iowa");
        STATE_MAP.put("KS", "Kansas");
        STATE_MAP.put("KY", "Kentucky");
        STATE_MAP.put("LA", "Louisiana");
        STATE_MAP.put("ME", "Maine");
        STATE_MAP.put("MB", "Manitoba");
        STATE_MAP.put("MD", "Maryland");
        STATE_MAP.put("MA", "Massachusetts");
        STATE_MAP.put("MI", "Michigan");
        STATE_MAP.put("MN", "Minnesota");
        STATE_MAP.put("MS", "Mississippi");
        STATE_MAP.put("MO", "Missouri");
        STATE_MAP.put("MT", "Montana");
        STATE_MAP.put("NE", "Nebraska");
        STATE_MAP.put("NV", "Nevada");
        STATE_MAP.put("NB", "New Brunswick");
        STATE_MAP.put("NH", "New Hampshire");
        STATE_MAP.put("NJ", "New Jersey");
        STATE_MAP.put("NM", "New Mexico");
        STATE_MAP.put("NY", "New York");
        STATE_MAP.put("NF", "Newfoundland");
        STATE_MAP.put("NC", "North Carolina");
        STATE_MAP.put("ND", "North Dakota");
        STATE_MAP.put("NT", "Northwest Territories");
        STATE_MAP.put("NS", "Nova Scotia");
        STATE_MAP.put("NU", "Nunavut");
        STATE_MAP.put("OH", "Ohio");
        STATE_MAP.put("OK", "Oklahoma");
        STATE_MAP.put("ON", "Ontario");
        STATE_MAP.put("OR", "Oregon");
        STATE_MAP.put("PA", "Pennsylvania");
        STATE_MAP.put("PE", "Prince Edward Island");
        STATE_MAP.put("PR", "Puerto Rico");
        STATE_MAP.put("QC", "Quebec");
        STATE_MAP.put("RI", "Rhode Island");
        STATE_MAP.put("SK", "Saskatchewan");
        STATE_MAP.put("SC", "South Carolina");
        STATE_MAP.put("SD", "South Dakota");
        STATE_MAP.put("TN", "Tennessee");
        STATE_MAP.put("TX", "Texas");
        STATE_MAP.put("UT", "Utah");
        STATE_MAP.put("VT", "Vermont");
        STATE_MAP.put("VI", "Virgin Islands");
        STATE_MAP.put("VA", "Virginia");
        STATE_MAP.put("WA", "Washington");
        STATE_MAP.put("WV", "West Virginia");
        STATE_MAP.put("WI", "Wisconsin");
        STATE_MAP.put("WY", "Wyoming");
        STATE_MAP.put("YT", "Yukon Territory");
    }

    private static final Set<String> STATE_CODES = STATE_MAP.keySet();
    private static final Collection<String> STATES = STATE_MAP.values();
    private static final List<String> NORTH_SOUTH_STATES = STATES.stream()
            .flatMap(state -> Stream.of("Southern " + state, "Northern " + state)).collect(Collectors.toList());

    private static final List<String> COUNTRIES = Arrays.stream(Locale.getISOCountries())
            .map(code -> new Locale("en", code).getDisplayCountry()).collect(Collectors.toList());

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
                        CONTINENTS, COUNTRIES, STATES, STATE_CODES, NORTH_SOUTH_STATES)) {
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
            } catch (RestClientException e) {
                LOGGER.error("Unable to process search results for " + searchUrl, e);
            }
        }
        return null;
    }

    private <T extends NasaMedia> List<T> doUpdateMedia(NasaMediaType mediaType) {
        return doUpdateMedia(mediaType, minYear, LocalDateTime.now().getYear());
    }

    private <T extends NasaMedia> List<T> doUpdateMedia(NasaMediaType mediaType, int year) {
        return doUpdateMedia(mediaType, year, year);
    }

    private <T extends NasaMedia> List<T> doUpdateMedia(NasaMediaType mediaType, int startYear, int endYear) {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting NASA {} update for years {}-{}...", mediaType, startYear, endYear);
        final List<T> medias = new ArrayList<>();
        RestTemplate rest = new RestTemplate();
        String nextUrl = searchEndpoint + "media_type=" + mediaType + "&year_start=" + startYear + "&year_end=" + endYear;
        while (nextUrl != null) {
            nextUrl = processSearchResults(rest, nextUrl, medias);
        }
        LOGGER.info("NASA {} update for years {}-{} completed: {} {}s in {}",
                mediaType, startYear, endYear, medias.size(), mediaType, Duration.between(LocalDateTime.now(), start));
        return medias;
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}")
    public List<NasaImage> updateImages() {
        List<NasaImage> images = new ArrayList<>();
        for (int year = LocalDateTime.now().getYear(); year >= minYear; year--) {
            images.addAll(doUpdateMedia(NasaMediaType.image, year));
        }
        return images;
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}")
    public List<NasaAudio> updateAudios() {
        return doUpdateMedia(NasaMediaType.audio);
    }

    @Scheduled(fixedRateString = "${nasa.update.rate}")
    public List<NasaVideo> updateVideos() {
        return doUpdateMedia(NasaMediaType.video);
    }

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
}
