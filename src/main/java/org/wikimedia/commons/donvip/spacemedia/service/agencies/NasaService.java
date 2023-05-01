package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.Collections.singleton;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaAudio;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.exception.WrappedIOException;
import org.wikimedia.commons.donvip.spacemedia.exception.WrappedUploadException;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMediaProcessorService.Counter;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataService;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class NasaService
        extends AbstractAgencyService<NasaMedia, String, ZonedDateTime, NasaMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaService.class);

    static final Pattern ISS_PATTERN = Pattern.compile("iss0{1,2}(\\d{1,2})e\\d{6}");

    static final Pattern ARTEMIS_PATTERN = Pattern.compile("art0{1,2}(\\d{1,2})e\\d{6}");

    static final Map<String, String> TWITTER_CENTER_ACCOUNTS = Map.ofEntries(entry("AFRC", "@nasaarmstrong"),
            entry("ARC", "@nasaames"), entry("GRC", "@nasaglenn"), entry("GSFC", "@NASAGoddard"),
            entry("HQ", "@nasahqphoto"), entry("JPL", "@NASAJPL"), entry("JSC", "@NASA_Johnson"),
            entry("KSC", "@NASAKennedy"), entry("LARC", "@NASA_Langley"), entry("LRC", "@NASA_Langley"),
            entry("MSFC", "@NASA_Marshall"), entry("SSC", "@NASAStennis"));

    @Value("${nasa.search.link}")
    private String searchEndpoint;

    @Value("${nasa.details.link}")
    private String detailsLink;

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
    private NasaMediaRepository<NasaMedia> mediaRepository;

    @Autowired
    private WikidataService wikidata;

    @Autowired
    private Environment env;

    @Autowired
    private NasaMediaProcessorService processor;

    private Map<String, String> nasaKeywords;

    @Autowired
    public NasaService(NasaMediaRepository<NasaMedia> repository) {
        super(repository, "nasa");
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        nasaKeywords = loadCsvMapping("nasa.keywords.csv");
    }

    @Override
    protected Class<NasaMedia> getMediaClass() {
        return NasaMedia.class;
    }

    @Override
    protected Class<NasaImage> getTopTermsMediaClass() {
        return NasaImage.class; // TODO can't get a direct lucene reader on NasaMedia
    }

    @Override
    protected final String getMediaId(String id) {
        return id;
    }

    @Override
    public NasaMedia saveMedia(NasaMedia media) {
        NasaMedia result;
        switch (media.getMediaType()) {
        case image:
            result = imageRepository.save((NasaImage) media);
            break;
        case video:
            result = videoRepository.save((NasaVideo) media);
            break;
        case audio:
            result = audioRepository.save((NasaAudio) media);
            break;
        default:
            throw new IllegalArgumentException(media.toString());
        }
        checkRemoteMedia(result);
        return result;
    }

    private <T extends NasaMedia> Pair<Integer, Collection<T>> doUpdateMedia(NasaMediaType mediaType) {
        return doUpdateMedia(mediaType, minYear, LocalDateTime.now().getYear(), null, null);
    }

    private <T extends NasaMedia> Pair<Integer, Collection<T>> doUpdateMedia(NasaMediaType mediaType, int year,
            Set<String> centers, Map<String, Set<String>> foundIds) {
        return doUpdateMedia(mediaType, year, year, centers, foundIds);
    }

    private <T extends NasaMedia> Pair<Integer, Collection<T>> doUpdateMedia(NasaMediaType mediaType, int startYear,
            int endYear, Set<String> centers, Map<String, Set<String>> foundIds) {
        LocalDateTime start = LocalDateTime.now();
        logStartUpdate(mediaType, startYear, endYear, centers);
        Counter count = new Counter();
        RestTemplate rest = new RestTemplate();
        rest.getMessageConverters().add(new NasaResponseHtmlErrorHandler());
        String nextUrl = searchEndpoint + "media_type=" + mediaType + "&year_start=" + startYear + "&year_end=" + endYear;
        if (centers != null) {
            nextUrl += "&center=" + String.join(",", centers);
        }
        Collection<T> uploadedMedia = new ArrayList<>();
        while (nextUrl != null) {
            String who = centers != null ? centers.toString() : getId();
            nextUrl = processor.processSearchResults(rest, nextUrl, uploadedMedia, count, who, foundIds,
                    this::ongoingUpdateMedia, this::doCommonUpdateUnchecked, this::shouldUploadAuto, this::problem,
                    this::saveMedia, this::saveMediaOrCheckRemote, this::uploadUnchecked);
        }
        logEndUpdate(mediaType, startYear, endYear, centers, start, count.count);
        return Pair.of(count.count, uploadedMedia);
    }

    private boolean doCommonUpdateUnchecked(NasaMedia media) {
        try {
            return doCommonUpdate(media);
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    private Triple<NasaMedia, Collection<Metadata>, Integer> uploadUnchecked(NasaMedia media, boolean checkUnicity,
            boolean isManual) {
        try {
            return upload(media, checkUnicity, isManual);
        } catch (UploadException e) {
            throw new WrappedUploadException(e);
        }
    }

    private static void logStartUpdate(NasaMediaType mediaType, int startYear, int endYear, Set<String> centers) {
        if (LOGGER.isDebugEnabled()) {
            if (centers == null) {
                if (startYear == endYear) {
                    LOGGER.debug("NASA {} update for year {} started...", mediaType, startYear);
                } else {
                    LOGGER.debug("NASA {} update for years {}-{} started...", mediaType, startYear, endYear);
                }
            } else if (startYear == endYear && centers.size() == 1) {
                LOGGER.debug("NASA {} update for year {} center {} started...", mediaType, startYear,
                        centers.iterator().next());
            } else {
                LOGGER.debug("NASA {} update for years {}-{} center {} started...", mediaType, startYear, endYear,
                        centers);
            }
        }
    }

    private static void logEndUpdate(NasaMediaType mediaType, int startYear, int endYear, Set<String> centers, LocalDateTime start, int size) {
        if (LOGGER.isDebugEnabled()) {
            Duration duration = Utils.durationInSec(start);
            if (centers == null) {
                if (startYear == endYear) {
                    LOGGER.debug("NASA {} update for year {} completed: {} {}s in {}", mediaType, startYear, size,
                            mediaType, duration);
                } else {
                    LOGGER.debug("NASA {} update for years {}-{} completed: {} {}s in {}", mediaType, startYear,
                            endYear, size, mediaType, duration);
                }
            } else if (startYear == endYear && centers.size() == 1) {
                LOGGER.debug("NASA {} update for year {} center {} completed: {} {}s in {}", mediaType, startYear,
                        centers.iterator().next(), size, mediaType, duration);
            } else {
                LOGGER.debug("NASA {} update for years {}-{} center {} completed: {} {}s in {}", mediaType, startYear,
                        endYear, centers.iterator().next(), size, mediaType, duration);
            }
        }
    }

    public Pair<Integer, Collection<NasaImage>> updateImages() {
        int count = 0;
        LocalDateTime start = LocalDateTime.now();
        List<NasaImage> uploadedImages = new ArrayList<>();
        Map<String, Set<String>> foundIds = nasaCenters.stream()
                .collect(toMap(Function.identity(), x -> new TreeSet<>()));
        // Recent years have a lot of photos: search by center to avoid more than 10k results
        for (int year = LocalDateTime.now().getYear(); year >= 2000; year--) {
            for (String center : nasaCenters) {
                Pair<Integer, Collection<NasaImage>> update = doUpdateMedia(NasaMediaType.image, year,
                        singleton(center), foundIds);
                uploadedImages.addAll(update.getRight());
                count += update.getLeft();
                ongoingUpdateMedia(start, count);
            }
            postSocialMedia(uploadedImages, uploadedImages.stream().map(Media::getMetadata).toList());
        }
        // Ancient years have a lot less photos: simple search for all centers
        for (int year = 1999; year >= minYear; year--) {
            Pair<Integer, Collection<NasaImage>> update = doUpdateMedia(NasaMediaType.image, year, null, foundIds);
            uploadedImages.addAll(update.getRight());
            count += update.getLeft();
            ongoingUpdateMedia(start, count);
        }
        // Delete media removed from NASA website
        for (String center : nasaCenters) {
            for (NasaImage image : imageRepository.findMissingInCommonsByCenterNotIn(center, foundIds.get(center))) {
                LOGGER.warn("TODO: deleting {} media removed from NASA website: {}", center, image);
                // imageRepository.delete(image);
            }
        }
        return Pair.of(count, uploadedImages);
    }

    public Pair<Integer, Collection<NasaAudio>> updateAudios() {
        return doUpdateMedia(NasaMediaType.audio);
    }

    public Pair<Integer, Collection<NasaVideo>> updateVideos() {
        return doUpdateMedia(NasaMediaType.video);
    }

    @Override
    public void updateMedia() {
        LocalDateTime start = startUpdateMedia();
        Collection<NasaMedia> uploadedMedia = new ArrayList<>();
        int count = 0;

        Pair<Integer, Collection<NasaImage>> images = updateImages();
        count += images.getLeft();
        uploadedMedia.addAll(images.getRight());

        Pair<Integer, Collection<NasaAudio>> audios = updateAudios();
        count += audios.getLeft();
        uploadedMedia.addAll(audios.getRight());

        if (videosEnabled) {
            Pair<Integer, Collection<NasaVideo>> videos = updateVideos();
            count += videos.getLeft();
            uploadedMedia.addAll(videos.getRight());
        }
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().map(Media::getMetadata).toList(), start, false);
    }

    @Override
    public String getName() {
        return "NASA";
    }

    @Override
    public URL getSourceUrl(NasaMedia media) throws MalformedURLException {
        return new URL(detailsLink.replace("<id>", media.getId()));
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaMedia media) {
        return Optional.ofNullable(media.getDate());
    }

    @Override
    protected String getAuthor(NasaMedia media) {
        String center = media.getCenter().toLowerCase(Locale.ENGLISH);
        URL homePage = env.getProperty("nasa." + center + ".home.page", URL.class);
        String name = env.getProperty("nasa." + center + ".name", String.class);
        if (media instanceof NasaImage image && StringUtils.isNotBlank(image.getPhotographer())) {
            name += " / " + image.getPhotographer();
        }
        return wikiLink(homePage, name);
    }

    @Override
    public Statistics getStatistics(boolean details) {
        Statistics stats = super.getStatistics(details);
        if (details) {
            List<String> centers = mediaRepository.findCenters();
            if (centers.size() > 1) {
                stats.setDetails(centers.parallelStream()
                        .map(c -> new Statistics(Objects.toString(c), Objects.toString(c),
                                mediaRepository.countByCenter(c),
                                mediaRepository.countUploadedToCommonsByCenter(c),
                                mediaRepository.countIgnoredByCenter(c),
                                mediaRepository.countMissingImagesInCommons(c),
                                mediaRepository.countMissingVideosInCommons(c),
                                mediaRepository.countByMetadata_PhashNotNullAndCenter(c), null))
                        .sorted().toList());
            }
        }
        return stats;
    }

    @Override
    protected String getSource(NasaMedia media) throws MalformedURLException {
        return "{{NASA-image|id=" + media.getId() + "|center=" + media.getCenter() + "}}";
    }

    @Override
    public Set<String> findCategories(NasaMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(media.getKeywords().stream().map(nasaKeywords::get).filter(Objects::nonNull).toList());
        String description = media.getDescription();
        Matcher idMatcher = ISS_PATTERN.matcher(media.getId());
        if (idMatcher.matches()) {
            String expedition = "Expedition " + idMatcher.group(1);
            result.add("ISS " + expedition);
            findIssExpeditionCrew(expedition).ifPresent(crew ->
                wikidata.mapCommonsCategoriesByFamilyName(crew).forEach((name, cat) -> {
                    if (description.contains(name) && !description.contains("Credit: " + cat)
                            && !description.contains("Credit: NASA/" + cat)) {
                        result.add(cat);
                    }
            }));
                if (List.of("in the cupola", "in the Cupola", "inside the cupola", "inside the Cupola",
                        "inside the newly-installed cupola", "inside the seven window cupola",
                        "in the seven-windowed Cupola", "from the Cupola", "through the cupola",
                        "inside the seven-windowed cupola").stream()
                        .anyMatch(description::contains)) {
                    result.add("Interior of Cupola (ISS module)");
                }
        } else {
            idMatcher = ARTEMIS_PATTERN.matcher(media.getId());
            if (idMatcher.matches()) {
                String artemis = "Artemis " + idMatcher.group(1);
                if (List.of("image of the planet", "photo of the Earth", "photo of Earth", "captures Earth",
                        "captured Earth", "captured the Earth", "image of our Earth", "image of the Earth",
                        "looking back at the Earth from a camera", "Earth, which appears").stream()
                        .anyMatch(description::contains)) {
                    result.add("Photos of Earth by " + artemis);
                } else if (List.of("photo of the Moon", "camera looked back at the Moon", "captured the Moon",
                        "Moon is captured", "Moon is seen", "the Moon looms", "these views of the Moon",
                        "image of the Moon", "image of the full Moon", "image of our Moon", "lunar image",
                        "imagery of the Moon", "Moon is in view", "captures the far side of the Moon",
                        "captured the far side of the Moon", "images of craters on the Moon",
                        "photo of the lunar surface", "Moon grows", "Moon appears", "imagery looking back at the Moon")
                        .stream().anyMatch(description::contains)) {
                    result.add("Photos of the Moon by " + artemis);
                } else {
                    result.add("Photos by " + artemis);
                }
                result.remove(artemis);
            }
        }
        return result;
    }

    private Optional<StatementGroup> findIssExpeditionCrew(String expedition) {
        Optional<StatementGroup> opt = Optional.empty();
        try {
            opt = wikidata.findCommonsStatementGroup("Category:ISS " + expedition, "P1029");
            if (opt.isEmpty()) {
                opt = wikidata.findWikipediaStatementGroup(expedition, "P1029");
            }
        } catch (IOException e) {
            LOGGER.error("Wikidata error", e);
        }
        return opt;
    }

    @Override
    public Set<String> findTemplates(NasaMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaMedia refresh(NasaMedia media) throws IOException {
        NasaResponse response = new RestTemplate().getForObject(searchEndpoint + "nasa_id=" + media.getId(),
                NasaResponse.class);
        if (response != null) {
            List<NasaItem> items = response.getCollection().getItems();
            if (items.size() == 1) {
                List<NasaMedia> data = items.get(0).getData();
                if (data.size() == 1) {
                    return media.copyDataFrom(data.get(0));
                }
            }
        }
        return media;
    }

    @Override
    protected Set<String> getEmojis(NasaMedia uploadedMedia) {
        Set<String> result = new HashSet<>();
        if (ISS_PATTERN.matcher(uploadedMedia.getId()).matches()) {
            result.add(Emojis.ASTRONAUT);
        }
        result.addAll(AbstractSocialMediaService.getEmojis(uploadedMedia.getKeywords()));
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaMedia uploadedMedia) {
        Set<String> result = new HashSet<>();
        if (ISS_PATTERN.matcher(uploadedMedia.getId()).matches()) {
            result.add("@Space_Station");
        } else if (uploadedMedia.getCenter() != null) {
            String account = TWITTER_CENTER_ACCOUNTS.get(uploadedMedia.getCenter());
            if (account != null) {
                result.add(account);
            }
        }
        if (result.isEmpty()) {
            result.add("@NASA");
        }
        return result;
    }
}
