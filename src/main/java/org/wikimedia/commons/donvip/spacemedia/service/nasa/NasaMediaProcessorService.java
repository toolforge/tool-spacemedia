package org.wikimedia.commons.donvip.spacemedia.service.nasa;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.service.MediaService.ignoreMedia;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ExifMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaAssets;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaCollection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaLink;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaResponse;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.utils.Geo;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NasaMediaProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaMediaProcessorService.class);

    /**
     * Minimal delay between successive API requests, in milliseconds.
     */
    private static final int DELAY = 3601;

    private static final Pattern PATTERN_NUMBER = Pattern.compile(".*\\d+,\\d+.*");
    private static final Pattern PATTERN_DATE = Pattern.compile(".*\\p{Alpha}+\\.? \\d{1,2}, [12]\\d{3}.*");
    private static final Pattern PATTERN_A_AND_B = Pattern.compile(".*[\\p{Alpha}\\.]+, (and|&) \\p{Alpha}+.*");
    private static final Pattern PATTERN_A_B_AND_C = Pattern.compile(".*\\p{Alpha}+, \\p{Alpha}+ (and|&) \\p{Alpha}+.*");
    private static final Pattern PATTERN_ER = Pattern.compile("[^,]*\\p{Alpha}+er, \\p{Alpha}+er[^,]*");

    @Value("${nasa.max.tries}")
    private int maxTries;

    @Value("${nasa.metadata.link}")
    private String metadataLink;

    @Autowired
    private ExifMetadataRepository exifRepository;

    @Autowired
    private NasaMediaRepository<NasaMedia> repository;

    @Autowired
    protected MediaService mediaService;

    @Autowired
    private ObjectMapper jackson;

    private LocalDateTime lastRequest;

    /**
     * Makes sure the service complies with api.nasa.gov hourly limit of 1,000
     * requests per hour
     */
    void ensureApiLimit() {
        LocalDateTime fourSecondsAgo = now().minus(DELAY, ChronoUnit.MILLIS);
        if (lastRequest != null && lastRequest.isAfter(fourSecondsAgo)) {
            try {
                long millis = MILLIS.between(now(), lastRequest.plus(DELAY, ChronoUnit.MILLIS));
                LOGGER.info("Sleeping {} ms to conform to NASA API limit policy", millis);
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        lastRequest = now();
    }

    @Transactional
    public <T extends NasaMedia> String processSearchResults(RestTemplate rest, String searchUrl,
            Collection<T> uploadedMedia, Counter counter, String who, Map<String, Set<String>> foundIds,
            TriConsumer<LocalDateTime, String, Integer> ongoingUpdateMedia,
            Predicate<NasaMedia> doCommonUpdate, BiPredicate<NasaMedia, Boolean> shouldUploadAuto,
            BiConsumer<URL, Throwable> problem, UnaryOperator<NasaMedia> saveMedia,
            BiFunction<Boolean, NasaMedia, NasaMedia> saveMediaOrCheckRemote,
            TriFunction<NasaMedia, Boolean, Boolean, Triple<NasaMedia, Collection<Metadata>, Integer>> uploader) {
        LocalDateTime start = LocalDateTime.now();
        // NASA website can return application/octet-stream instead of application/json
        RestTemplate restExif = Utils.restTemplateSupportingAll(jackson);
        boolean ok = false;
        int count = 0;
        for (int i = 0; i < maxTries && !ok; i++) {
            try {
                ensureApiLimit();
                LOGGER.debug("Fetching {}", searchUrl);
                NasaCollection collection = rest.getForObject(searchUrl, NasaResponse.class).getCollection();
                List<NasaItem> items = collection.getItems();
                ok = true;
                for (NasaItem item : items) {
                    try {
                        Pair<NasaMedia, Integer> update = processMedia(rest, restExif, item.getData().get(0),
                                item.getHref(),
                                doCommonUpdate, shouldUploadAuto, problem, saveMedia, saveMediaOrCheckRemote, uploader);
                        @SuppressWarnings("unchecked")
                        T media = (T) update.getKey();
                        if (update.getValue() > 0) {
                            uploadedMedia.add(media);
                        }
                        if (foundIds != null) {
                            foundIds.get(media.getCenter()).add(media.getId());
                        }
                        ongoingUpdateMedia.accept(start, who, count++);
                        counter.count++;
                    } catch (Forbidden e) {
                        problem.accept(item.getHref(), e);
                    } catch (RestClientException e) {
                        if (e.getCause() instanceof HttpMessageNotReadableException) {
                            problem.accept(item.getHref(), e.getCause());
                        } else {
                            LOGGER.error("Cannot process item {}", item, e);
                        }
                    } catch (IOException | URISyntaxException | RuntimeException e) {
                        LOGGER.error("Cannot process item {}", item, e);
                    }
                }
                if (!CollectionUtils.isEmpty(collection.getLinks())) {
                    Optional<NasaLink> next = collection.getLinks().stream().filter(l -> "next".equals(l.getRel()))
                            .findFirst();
                    if (next.isPresent()) {
                        // API returns http links with 301 redirect in text/html
                        // not correctly handled by RestTemplate, so switch to https
                        return next.get().getHref().toExternalForm().replace("http://", "https://");
                    }
                }
            } catch (RestClientException e) {
                LOGGER.error("Unable to process search results for {}", searchUrl, e);
            }
        }
        return null;
    }

    private Pair<NasaMedia, Integer> processMedia(RestTemplate rest, RestTemplate restExif, NasaMedia media, URL href,
            Predicate<NasaMedia> doCommonUpdate, BiPredicate<NasaMedia, Boolean> shouldUploadAuto,
            BiConsumer<URL, Throwable> problem, UnaryOperator<NasaMedia> saveMedia,
            BiFunction<Boolean, NasaMedia, NasaMedia> saveMediaOrCheckRemote,
            TriFunction<NasaMedia, Boolean, Boolean, Triple<NasaMedia, Collection<Metadata>, Integer>> uploader)
            throws IOException, URISyntaxException {
        Optional<NasaMedia> mediaInRepo = repository.findById(media.getId());
        boolean save = false;
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            save = true;
            // API is supposed to send us keywords in a proper JSON array, but not always
            Set<String> normalizedKeywords = normalizeKeywords(media.getKeywords());
            if (!Objects.equals(normalizedKeywords, media.getKeywords())) {
                media.setKeywords(normalizedKeywords);
            }
            if (media.getAssetUrl() == null) {
                findOriginalMedia(rest, href).ifPresent(media::setAssetUrl);
            }
            if (media.getThumbnailUrl() == null) {
                findThumbnailMedia(rest, href).ifPresent(media::setThumbnailUrl);
            }
            if (media.getTitle() != null && media.getTitle().startsWith("Title: ")) {
                media.setTitle(media.getTitle().replace("Title: ", ""));
            }
            if (media.getId().length() < 3) {
                problem.accept(media.getAssetUrl(), new Exception("Strange id: '" + media.getId() + "'"));
            }
            if (media.isIgnored() != Boolean.TRUE && media.getDescription() != null) {
                if (media.getDescription().contains("/photojournal")) {
                    ignoreMedia(media, "Photojournal");
                    save = true;
                } else {
                    String desc = media.getDescription().toLowerCase(Locale.ENGLISH);
                    if (desc.contains("courtesy") || desc.contains("©")) {
                        ignoreMedia(media, "Probably non-free image (courtesy)");
                        save = true;
                    }
                }
            }
        }
        if (media instanceof NasaImage img && img.isIgnored() != Boolean.TRUE && img.getPhotographer() != null
                && mediaService.isPhotographerBlocklisted(img.getPhotographer())) {
            ignoreMedia(media, "Non-NASA image, photographer blocklisted: " + img.getPhotographer());
            save = true;
        }
        if (doCommonUpdate.test(media)) {
            save = true;
        }
        if (media.getMetadata() != null && media.getMetadata().getExif() == null) {
            try {
                ExifMetadata exifMetadata = readExifMetadata(restExif, media.getId());
                if (exifMetadata != null) {
                    media.getMetadata().setExif(exifRepository.save(exifMetadata));
                    save = true;
                }
            } catch (HttpClientErrorException.Forbidden e) {
                // NHQ202211160205-2 always return http 403
                LOGGER.error("Unable to retrieve EXIF metadata for {}: {}", media.getId(), e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unable to retrieve EXIF metadata for {}", media.getId(), e);
            }
        }
        int uploadCount = 0;
        if (shouldUploadAuto.test(media, false)) {
            Triple<NasaMedia, Collection<Metadata>, Integer> upload = uploader
                    .apply(save ? saveMedia.apply(media) : media, true, false);
            uploadCount += upload.getRight();
            media = saveMedia.apply(upload.getLeft());
            save = false;
        }
        return Pair.of(saveMediaOrCheckRemote.apply(save, media), uploadCount);
    }

    ExifMetadata readExifMetadata(RestTemplate restExif, String id)
            throws RestClientException, MalformedURLException, URISyntaxException {
        return restExif.getForObject(Utils.urlToUri(new URL(metadataLink.replace("<id>", id))), ExifMetadata.class);
    }

    private static Optional<URL> findSpecificMedia(RestTemplate rest, URL href, String text) throws URISyntaxException {
        NasaAssets assets = rest.getForObject(Utils.urlToUri(href), NasaAssets.class);
        return assets != null ? assets.stream().filter(u -> u.toExternalForm().contains(text)).findFirst()
                : Optional.empty();
    }

    static Optional<URL> findOriginalMedia(RestTemplate rest, URL href) throws URISyntaxException {
        return findSpecificMedia(rest, href, "~orig.");
    }

    static Optional<URL> findThumbnailMedia(RestTemplate rest, URL href) throws URISyntaxException {
        return findSpecificMedia(rest, href, "~thumb.");
    }

    public static Set<String> normalizeKeywords(Set<String> keywords) {
        if (keywords != null && keywords.size() == 1) {
            return doNormalizeKeywords(keywords);
        } else if (keywords != null) {
            // Look for bad situations like https://images.nasa.gov/details/GRC-2017-CM-0155
            // Keyword 1 : GRC-CM => Good :)
            // Keyword 2 : Solar Eclipse, Jefferson City Missouri, ... Reggie Williams,
            // Astronaut Mike Hopkins ==> WTF !?
            Set<String> normalized = new HashSet<>();
            for (Iterator<String> it = keywords.iterator(); it.hasNext();) {
                String kw = it.next();
                if (kw.length() > 300 && StringUtils.countMatches(kw, ",") > 10) {
                    normalized.addAll(doNormalizeKeywords(singleton(kw)));
                    it.remove();
                }
            }
            keywords.addAll(normalized);
        }
        return keywords;
    }

    private static Set<String> doNormalizeKeywords(Set<String> keywords) {
        String kw = keywords.iterator().next();
        for (String sep : Arrays.asList(",", ";")) {
            if (kw.contains(sep) && looksLikeMultipleValues(kw, sep)) {
                return Arrays.stream(kw.split(sep)).map(String::trim).filter(s -> !s.isEmpty()).collect(toSet());
            }
        }
        return keywords;
    }

    private static boolean looksLikeMultipleValues(String kw, String sep) {
        if (",".equals(sep)) {
            if (kw.startsWith("Hi, ") || kw.contains(", by ")) {
                return false;
            }
            if (kw.endsWith(sep)) {
                kw = kw.substring(0, kw.length() - sep.length());
            }
            if (kw.contains(sep)) {
                String after = kw.substring(kw.lastIndexOf(sep) + sep.length() + " ".length());
                for (Collection<String> entities : Arrays.asList(Geo.CONTINENTS, Geo.COUNTRIES, Geo.STATES,
                        Geo.STATE_CODES, Geo.NORTH_SOUTH_STATES)) {
                    if (entities.contains(after)) {
                        return false;
                    }
                }
                for (Pattern pattern : Arrays.asList(PATTERN_NUMBER, PATTERN_DATE, PATTERN_A_AND_B, PATTERN_A_B_AND_C,
                        PATTERN_ER)) {
                    if (pattern.matcher(kw).matches()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static class Counter {
        public int count = 0;
    }
}
