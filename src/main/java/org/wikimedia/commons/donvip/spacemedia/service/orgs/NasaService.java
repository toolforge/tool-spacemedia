package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Collections.singleton;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.durationInSec;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaAudio;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.exception.WrappedUploadException;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMediaProcessorService.Counter;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataService;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaService extends AbstractOrgService<NasaMedia> {

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

    private final Set<String> nasaCenters;

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
    public NasaService(NasaMediaRepository<NasaMedia> repository, @Value("${nasa.centers}") Set<String> nasaCenters) {
        super(repository, "nasa", nasaCenters);
        this.nasaCenters = Objects.requireNonNull(nasaCenters);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        nasaKeywords = loadCsvMapping("nasa.keywords.csv");
    }

    @Override
    protected boolean isNASA(NasaMedia media) {
        return true;
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
    public NasaMedia saveMedia(NasaMedia media) {
        LOGGER.info("Saving {}", media);
        if (media.getPublicationDateTime() == null) {
            // Not real, but the API doesn't provide the publication date, and we need it
            media.setPublicationDateTime(media.getCreationDateTime());
        }
        NasaMedia result = switch (media.getMediaType()) {
        case image -> imageRepository.save((NasaImage) media);
        case video -> videoRepository.save((NasaVideo) media);
        case audio -> audioRepository.save((NasaAudio) media);
        };
        if (result == null) {
            throw new IllegalArgumentException(media.toString());
        }
        checkRemoteMedia(result);
        return result;
    }

    @Override
    protected boolean ignoreExifMetadata() {
        // NASA has its own REST API to retrieve file metadata.
        // Much faster than reading EXIF from image itself
        return true;
    }

    private <T extends NasaMedia> Pair<Integer, Collection<T>> doUpdateMedia(NasaMediaType mediaType,
            LocalDate doNotFetchEarlierThan) {
        return doUpdateMedia(mediaType,
                doNotFetchEarlierThan != null ? Math.max(minYear, doNotFetchEarlierThan.getYear()) : minYear,
                LocalDateTime.now().getYear(), null, null, doNotFetchEarlierThan);
    }

    private <T extends NasaMedia> Pair<Integer, Collection<T>> doUpdateMedia(NasaMediaType mediaType, int year,
            Set<String> centers, Set<CompositeMediaId> foundIds, LocalDate doNotFetchEarlierThan) {
        return doUpdateMedia(mediaType, year, year, centers, foundIds, doNotFetchEarlierThan);
    }

    private <T extends NasaMedia> Pair<Integer, Collection<T>> doUpdateMedia(NasaMediaType mediaType, int startYear,
            int endYear, Set<String> centers, Set<CompositeMediaId> foundIds, LocalDate doNotFetchEarlierThan) {
        Counter count = new Counter();
        Collection<T> uploadedMedia = new ArrayList<>();
        if (doNotFetchEarlierThan == null || endYear >= doNotFetchEarlierThan.getYear()) {
            LocalDateTime start = LocalDateTime.now();
            logStartUpdate(mediaType, startYear, endYear, centers);
            RestTemplate rest = new RestTemplate();
            rest.getMessageConverters().add(new NasaResponseHtmlErrorHandler());
            String nextUrl = searchEndpoint + "media_type=" + mediaType + "&year_start=" + startYear + "&year_end="
                    + endYear;
            if (centers != null) {
                nextUrl += "&center=" + String.join(",", centers);
            }
            while (nextUrl != null) {
                String who = centers != null ? centers.toString() : getId();
                nextUrl = processor.processSearchResults(rest, nextUrl, uploadedMedia, count, who, foundIds,
                        this::ongoingUpdateMedia, this::doCommonUpdateUnchecked, this::shouldUploadAuto, this::problem,
                        this::saveMedia, this::saveMediaOrCheckRemote, this::uploadUnchecked);
            }
            logEndUpdate(mediaType, startYear, endYear, centers, start, count.count);
        }
        return Pair.of(count.count, uploadedMedia);
    }

    private boolean doCommonUpdateUnchecked(NasaMedia media) {
        try {
            return doCommonUpdate(media);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Triple<NasaMedia, Collection<FileMetadata>, Integer> uploadUnchecked(NasaMedia media, boolean checkUnicity,
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
            Duration duration = durationInSec(start);
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
        Set<CompositeMediaId> foundIds = new TreeSet<>();
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        // Recent years have a lot of photos: search by center to avoid more than 10k results
        for (int year = LocalDateTime.now().getYear(); year >= 2000; year--) {
            for (String center : nasaCenters) {
                List<NasaImage> localUploadedImages = new ArrayList<>();
                Pair<Integer, Collection<NasaImage>> update = doUpdateMedia(NasaMediaType.image, year,
                        singleton(center), foundIds, doNotFetchEarlierThan);
                localUploadedImages.addAll(update.getRight());
                count += update.getLeft();
                ongoingUpdateMedia(start, count);
                uploadedImages.addAll(localUploadedImages);
                postSocialMedia(localUploadedImages,
                        localUploadedImages.stream().flatMap(Media::getMetadataStream).toList());
            }
        }
        // Ancient years have a lot less photos: simple search for all centers
        for (int year = 1999; year >= minYear; year--) {
            Pair<Integer, Collection<NasaImage>> update = doUpdateMedia(NasaMediaType.image, year, null, foundIds,
                    doNotFetchEarlierThan);
            uploadedImages.addAll(update.getRight());
            count += update.getLeft();
            ongoingUpdateMedia(start, count);
        }
        if (doNotFetchEarlierThan == null) {
            // Delete media removed from NASA website (only for complete updates)
            for (String center : nasaCenters) {
                for (NasaImage image : imageRepository.findMissingInCommonsNotIn(Set.of(center),
                        foundIds.stream().filter(x -> x.getRepoId().equals(center)).map(CompositeMediaId::getMediaId)
                                .collect(toSet()))) {
                    LOGGER.warn("TODO: deleting {} media removed from NASA website: {}", center, image);
                    // imageRepository.delete(image);
                }
            }
        }
        return Pair.of(count, uploadedImages);
    }

    public Pair<Integer, Collection<NasaAudio>> updateAudios() {
        return doUpdateMedia(NasaMediaType.audio, getRuntimeData().getDoNotFetchEarlierThan());
    }

    public Pair<Integer, Collection<NasaVideo>> updateVideos() {
        return doUpdateMedia(NasaMediaType.video, getRuntimeData().getDoNotFetchEarlierThan());
    }

    @Override
    public void updateMedia() {
        LocalDateTime start = startUpdateMedia();
        Collection<NasaMedia> uploadedMedia = new ArrayList<>();
        int count = 0;

        Pair<Integer, Collection<NasaImage>> images = updateImages();
        count += images.getLeft();
        uploadedMedia.addAll(images.getRight());

        if (audiosEnabled) {
            Pair<Integer, Collection<NasaAudio>> audios = updateAudios();
            count += audios.getLeft();
            uploadedMedia.addAll(audios.getRight());
        }

        if (videosEnabled) {
            Pair<Integer, Collection<NasaVideo>> videos = updateVideos();
            count += videos.getLeft();
            uploadedMedia.addAll(videos.getRight());
        }
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().flatMap(Media::getMetadataStream).toList(),
                start, LocalDate.now().minusYears(1), // NASA sometimes post old images dating a few months back
                false /* tweets already posted - one by NASA center */);
    }

    @Override
    public String getName() {
        return "NASA";
    }

    @Override
    public URL getSourceUrl(NasaMedia media, FileMetadata metadata) {
        return newURL(detailsLink.replace("<id>", media.getId().getMediaId()));
    }

    @Override
    protected String getAuthor(NasaMedia media) {
        String center = media.getId().getRepoId().toLowerCase(Locale.ENGLISH);
        URL homePage = env.getProperty("nasa." + center + ".home.page", URL.class);
        String name = env.getProperty("nasa." + center + ".name", String.class);
        if (media instanceof NasaImage image) {
            if (isNotBlank(image.getPhotographer())) {
                name += " / " + image.getPhotographer();
            }
            if (isNotBlank(image.getSecondaryCreator())) {
                name += " / " + image.getSecondaryCreator();
            }
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
                                mediaRepository.count(Set.of(c)),
                                mediaRepository.countUploadedToCommons(Set.of(c)),
                                mediaRepository.countByIgnoredTrue(Set.of(c)),
                                mediaRepository.countMissingImagesInCommons(Set.of(c)),
                                mediaRepository.countMissingVideosInCommons(Set.of(c)),
                                mediaRepository.countByMetadata_PhashNotNull(Set.of(c)), null))
                        .sorted().toList());
            }
        }
        return stats;
    }

    @Override
    protected String getSource(NasaMedia media, FileMetadata metadata) {
        return "{{NASA-image|id=" + media.getId().getMediaId() + "|center=" + media.getId().getRepoId() + "}}";
    }

    @Override
    protected String getTakenLocation(NasaMedia media) {
        return ISS_PATTERN.matcher(media.getId().getMediaId()).matches() ? "ISS" : "";
    }

    @Override
    public Set<String> findCategories(NasaMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(media.getKeywords().stream().map(nasaKeywords::get).filter(Objects::nonNull).toList());
        String description = media.getDescription();
        Matcher idMatcher = ISS_PATTERN.matcher(media.getId().getMediaId());
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
                        "inside the seven-windowed cupola", "out of the cupola",
                        "inside the International Space Station's \"window to the world").stream()
                        .anyMatch(description::contains)) {
                    result.add("Interior of Cupola (ISS module)");
                }
        } else {
            idMatcher = ARTEMIS_PATTERN.matcher(media.getId().getMediaId());
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
    public Set<String> findLicenceTemplates(NasaMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaMedia refresh(NasaMedia media) throws IOException {
        RestTemplate rest = new RestTemplate();
        NasaResponse response = rest.getForObject(searchEndpoint + "nasa_id=" + media.getId().getMediaId(),
                NasaResponse.class);
        if (response != null) {
            List<NasaItem> items = response.getCollection().getItems();
            if (items.size() == 1) {
                NasaItem item = items.get(0);
                List<NasaMedia> data = item.getData();
                if (data.size() == 1) {
                    try {
                        NasaMedia mediaFromApi = data.get(0);
                        processor.processMediaFromApi(rest, mediaFromApi, item.getHref(), this::problem, false);
                        if (mediaFromApi.hasMetadata() && mediaFromApi.getAssetUrl() != null) {
                            LOGGER.info("Copying up-to-date data from {}", mediaFromApi);
                            return media.copyDataFrom(mediaFromApi);
                        }
                    } catch (URISyntaxException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        return null; // delete
    }

    @Override
    protected Set<String> getEmojis(NasaMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        if (ISS_PATTERN.matcher(uploadedMedia.getId().getMediaId()).matches()) {
            result.add(Emojis.ASTRONAUT);
        }
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaMedia uploadedMedia) {
        Set<String> result = new HashSet<>();
        if (ISS_PATTERN.matcher(uploadedMedia.getId().getMediaId()).matches()) {
            result.add("@Space_Station");
        } else if (uploadedMedia.getId().getRepoId() != null) {
            String account = TWITTER_CENTER_ACCOUNTS.get(uploadedMedia.getId().getRepoId());
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
