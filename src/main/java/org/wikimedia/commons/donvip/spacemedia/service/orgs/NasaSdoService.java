package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoInstrument.AIA;
import static org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoInstrument.EVE;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q115801008_MAGNETOGRAM;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q119021644_INTENSITYGRAM;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q125191_PHOTOGRAPH;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q5297355_DOPPLERGRAM;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q98069877_VIDEO;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.executeRequest;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoDataType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoInstrument;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateContext;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaSdoService extends AbstractOrgService<NasaSdoMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaSdoService.class);

    private static final String BASE_URL = "https://sdo.gsfc.nasa.gov";
    private static final String BASE_URL_IMG = BASE_URL + "/assets/img";
    private static final String JSOC_BASE_URL = "http://jsoc.stanford.edu/data";
    private static final String SAM_BASE_URL = "https://lasp.colorado.edu/eve/data_access/eve_data/quicklook/L0CS/SAM";

    static final DateTimeFormatter IMG_NAME_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    static final DateTimeFormatter AIA_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSX");
    static final DateTimeFormatter HMI_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH:mm:ss.SSX");
    static final DateTimeFormatter SAM_IMG_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/DDD/01'h'");
    static final DateTimeFormatter SAM_VID_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    static final DateTimeFormatter SAM_FIL_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyDDD");

    @Autowired
    private NasaSdoMediaRepository sdoRepository;

    public NasaSdoService(NasaSdoMediaRepository repository) {
        super(repository, "nasa.sdo",
                Arrays.stream(NasaSdoInstrument.values()).map(NasaSdoInstrument::name).collect(toSet()));
    }

    @Override
    protected boolean addNASAVideoCategory() {
        // SDO template adds more accurate categories for videos
        return false;
    }

    @Override
    protected boolean needsReview() {
        return false;
    }

    @Override
    protected Class<NasaSdoMedia> getMediaClass() {
        return NasaSdoMedia.class;
    }

    @Override
    public String getName() {
        return "NASA (SDO)";
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected boolean includeByPerceptualHash() {
        return false;
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia SDO files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected List<String> getReviewCategories() {
        return List.of();
    }

    @Override
    public URL getSourceUrl(NasaSdoMedia media, FileMetadata metadata, String ext) {
        return metadata.getAssetUrl();
    }

    @Override
    protected String getAuthor(NasaSdoMedia media, FileMetadata metadata) {
        // https://sdo.gsfc.nasa.gov/data/rules.php
        return "Courtesy of NASA/SDO and the AIA, EVE, and HMI science teams.";
    }

    @Override
    protected SdcStatements getStatements(NasaSdoMedia media, FileMetadata metadata) {
        NasaSdoDataType dataType = media.getDataType();
        SdcStatements result = super.getStatements(media, metadata).instanceOf(switch (dataType) {
        case _HMID -> Q5297355_DOPPLERGRAM;
        case _HMIB, _HMIBC -> Q115801008_MAGNETOGRAM;
        case _HMII, _HMIIC, _HMIIF -> Q119021644_INTENSITYGRAM;
        default -> metadata.isVideo() ? Q98069877_VIDEO : Q125191_PHOTOGRAPH;
        });
        result.creator("Q382494") // Created by SDO
                .depicts("Q525") // Depicts the Sun
                .locationOfCreation("Q472251") // Created in geosynchronous orbit
                .fabricationMethod(Q725252_SATELLITE_IMAGERY)
                .capturedWith(dataType.getInstrument().getQid()); // Taken with SDO instrument
        int wavelength = dataType.getWavelength();
        if (wavelength > 0) {
            result.put("P2808", Pair.of(Pair.of(wavelength, "Q81454"), null)); // Wavelength in ångström (Å)
        }
        addSdcKeywordsStatements(media.getKeywords(), result);
        return result;
    }

    private static void addSdcKeywordsStatements(NasaSdoKeywords keywords, SdcStatements result) {
        Optional.ofNullable(keywords).map(NasaSdoKeywords::getExpTime) // Exposure time in seconds
                .ifPresent(exp -> result.put("P6757", Pair.of(Pair.of(exp, "Q11574"), null)));
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaSdoMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        for (LocalDate date = getLocalDateFromArgs(args); date.getYear() >= 2010
                && (doNotFetchEarlierThan == null || date.isAfter(doNotFetchEarlierThan)); date = date.minusDays(1)) {
            String dateStringPath = DateTimeFormatter.ISO_LOCAL_DATE.format(date).replace('-', '/');
            count += updateImages(dateStringPath, date, uploadedMedia, start, count);
            if (videosEnabled) {
                count += updateVideos(dateStringPath, date, uploadedMedia, start, count);
            }
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private static List<NasaSdoKeywords> fetchAiaKeywords(LocalDate date) throws IOException {
        return fetchKeywords(AIA, date, AIA_DATE_TIME_FORMAT);
    }

    private static List<NasaSdoKeywords> fetchKeywords(NasaSdoInstrument instrument, LocalDate date,
            DateTimeFormatter dateTimeFormatter) throws IOException {
        URL keywordsUrl = getKeywordsUrl(instrument, date);
        LOGGER.info("Fetching {}", keywordsUrl);
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                ClassicHttpResponse response = executeRequest(newHttpGet(keywordsUrl), httpclient, null, false);
                InputStream in = response.getEntity().getContent()) {
            if (response.getCode() >= 400) {
                LOGGER.warn("{} => {}", keywordsUrl, response);
                return List.of();
            }
            return parseKeywords(in, dateTimeFormatter);
        }
    }

    private static URL getKeywordsUrl(NasaSdoInstrument instrument, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        return newURL(String.format("%s/%s/lev0_keywords/%04d/%02d/%04d.%02d.%02d.txt", JSOC_BASE_URL,
                instrument.name().toLowerCase(Locale.ENGLISH), year, month, year, month, day));
    }

    static List<NasaSdoKeywords> parseKeywords(InputStream in, DateTimeFormatter dateTimeFormatter) throws IOException {
        List<NasaSdoKeywords> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, UTF_8));) {
            String line = br.readLine();
            if (line != null) {
                int obsIndex = line.indexOf("T_OBS");
                int fsnIndex = line.indexOf("FSN");
                int camIndex = line.indexOf("CAMERA");
                int expIndex = line.indexOf("EXPTIME");
                int wavIndex = line.indexOf("WAVELNTH");
                if (obsIndex >= 0 && fsnIndex >= 0) {
                    Function<String, ZonedDateTime> dateTimeParser = s -> ZonedDateTime.parse(s.replace("_UTC", "Z"),
                            dateTimeFormatter);
                    while ((line = br.readLine()) != null) {
                        NasaSdoKeywords kw = new NasaSdoKeywords();
                        extractKeyword(line, obsIndex, dateTimeParser).ifPresent(kw::settObs);
                        extractKeyword(line, fsnIndex, Integer::valueOf).ifPresent(kw::setFsn);
                        extractKeyword(line, camIndex, Short::valueOf).ifPresent(kw::setCamera);
                        extractKeyword(line, expIndex, Double::valueOf).ifPresent(kw::setExpTime);
                        extractKeyword(line, wavIndex, Short::valueOf).ifPresent(kw::setWavelength);
                        result.add(kw);
                    }
                }
            }
        }
        return result;
    }

    private static <T> Optional<T> extractKeyword(String line, int index, Function<String, T> mapper) {
        try {
            return index >= 0 ? Optional.of(mapper.apply(line.substring(index, line.indexOf(' ', index))))
                : Optional.empty();
        } catch (DateTimeParseException | NumberFormatException e) {
            LOGGER.error("Cannot parse line: " + line, e);
            GlitchTip.capture(e);
            return Optional.empty();
        }
    }

    // First image of each day
    private int updateImages(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count) throws IOException {
        List<NasaSdoKeywords> aiaKeywords = sdoRepository.existsByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(
                NasaMediaType.image, NasaSdoDataType.values(AIA), date) ? fetchAiaKeywords(date) : List.of();
        // HMI keywords are not fetched as it is not clear how to match level 0 keywords
        // with HMI 15 minutes catalog
        int eveCount = updateEveImagesOrVideos(date, NasaMediaType.image, "images", "png", uploadedMedia, start, count);
        return updateAiaHmiImagesOrVideos(dateStringPath, date, 4096, NasaMediaType.image, "browse", "jpg", aiaKeywords,
                null, uploadedMedia, start, count + eveCount, NasaSdoService::parseImageDateTime);
    }

    static ZonedDateTime parseImageDateTime(String filename) {
        return LocalDateTime.parse(filename.substring(0, 15), IMG_NAME_DATE_TIME_FORMAT).atZone(ZoneId.of("UTC"));
    }

    // Daily movies
    private int updateVideos(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count) throws IOException {
        // TODO other mpeg/mp4 videos at JSOC/LASP
        // http://jsoc.stanford.edu/HMI/hmiimage.html
        // http://jsoc.stanford.edu/data/hmi/images/2023/05/01/Ic_flat_2d.mpg
        // http://jsoc.stanford.edu/data/hmi/images/2023/05/01/M_2d.mpg
        // http://jsoc.stanford.edu/data/hmi/images/2023/05/01/M_color_2d.mpg
        // https://lasp.colorado.edu/eve/data_access/eve_data/quicklook/L0CS/SAM/video/daily/2014/SAM_L2_2013365_2014001_004_01.mp4
        int eveCount = updateEveImagesOrVideos(date, NasaMediaType.video, "video/daily", "mp4", uploadedMedia, start, count);
        return updateAiaHmiImagesOrVideos(dateStringPath, date, 1024, NasaMediaType.video, "dailymov", "ogv", null,
                null, uploadedMedia, start, count + eveCount, NasaSdoService::parseMovieDateTime);
    }

    static ZonedDateTime parseMovieDateTime(String filename) {
        return LocalDate.parse(filename.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay(ZoneId.of("UTC"));
    }

    private int updateEveImagesOrVideos(LocalDate date, NasaMediaType mediaType, String browse, String ext,
            List<NasaSdoMedia> uploadedMedia, LocalDateTime start, int count) throws IOException {
        return updateImagesOrVideos(date, LocalDate.of(2014, 5, 31),
                mediaType == NasaMediaType.video ? 0 : 1 /* Only mp4 (TODO) */, new ImageDimensions(320, 240),
                mediaType,
                String.join("/", SAM_BASE_URL, browse,
                        (mediaType == NasaMediaType.video ? SAM_VID_DATE_TIME_FORMAT : SAM_IMG_DATE_TIME_FORMAT)
                                .format(date)),
                "_" + SAM_FIL_DATE_TIME_FORMAT.format(date) + "_",
                (i, dataType) -> i.startsWith(dataType.name()) && i.endsWith('.' + ext), ext,
                uploadedMedia, start, count, x -> date);
    }

    private int updateAiaHmiImagesOrVideos(String dateStringPath, LocalDate date, int dim, NasaMediaType mediaType,
            String browse, String ext, List<NasaSdoKeywords> aiaKeywords, List<NasaSdoKeywords> hmiKeywords,
            List<NasaSdoMedia> uploadedMedia, LocalDateTime start, int count,
            Function<String, ZonedDateTime> dateTimeExtractor) throws IOException {
        if (mediaType == NasaMediaType.image && date.getDayOfMonth() == 1) {
            LOGGER.info("Current update date: {} ...", date);
        }
        ImageDimensions dims = new ImageDimensions(dim, dim);
        int localCount = updateImagesOrVideos(date, LocalDate.now().plusDays(1),
                mediaType == NasaMediaType.video ? NasaSdoDataType.values().length - 2
                        : NasaSdoDataType.values().length - 1,
                dims, mediaType, String.join("/", BASE_URL_IMG, browse, dateStringPath), "_" + dims.getWidth() + "_",
                (i, dataType) -> i.endsWith(dataType.name() + '.' + ext), ext, uploadedMedia, start, count,
                dateTimeExtractor);
        updateKeywords(date, mediaType, aiaKeywords, hmiKeywords, dims);
        return localCount;
    }

    private int updateImagesOrVideos(LocalDate date, LocalDate maxValidityDate, int expectedCount, ImageDimensions dims,
            NasaMediaType mediaType, String browseUrl, String textToFind,
            BiPredicate<String, NasaSdoDataType> fileFilter, String ext, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count, Function<String, ? extends Temporal> dateTimeExtractor) throws IOException {
        int localCount = 0;
        if (date.isBefore(maxValidityDate)) {
            long imagesUploadedInDb = sdoRepository.countUploadedByMediaTypeAndDimensionsAndDate(mediaType, dims, date);
            if (imagesUploadedInDb < expectedCount) {
                List<NasaSdoMedia> imagesNotUploadedInDb = sdoRepository
                        .findMissingByMediaTypeAndDimensionsAndDate(mediaType, dims, date);
                for (NasaSdoMedia media : imagesNotUploadedInDb) {
                    doUpdateMedia(media, false, uploadedMedia);
                    ongoingUpdateMedia(start, count + localCount++);
                }
                if (imagesNotUploadedInDb.size() < expectedCount) {
                    LOGGER.info("Fetching {} ({}<{})", browseUrl, imagesNotUploadedInDb.size(), expectedCount);
                    try {
                        List<String> files = fetchFiles(browseUrl, textToFind);
                        for (NasaSdoDataType dataType : NasaSdoDataType.values()) {
                            Optional<String> opt = files.stream().filter(i -> fileFilter.test(i, dataType)).findFirst();
                            if (opt.isPresent()) {
                                String firstFile = opt.get();
                                updateMedia(
                                        new CompositeMediaId(dataType.getInstrument().name(),
                                                firstFile.replace("." + ext, "")),
                                        dateTimeExtractor.apply(firstFile), dims, newURL(browseUrl + '/' + firstFile),
                                        mediaType, dataType, uploadedMedia);
                                ongoingUpdateMedia(start, count + localCount++);
                            }
                        }
                    } catch (HttpStatusException e) {
                        // https://sdo.gsfc.nasa.gov/assets/img/browse/2016/12/25/ => HTTP 404
                        LOGGER.error(e.getMessage(), e);
                        GlitchTip.capture(e);
                    }
                }
            } else {
                for (NasaSdoMedia media : sdoRepository.findByMediaTypeAndDimensionsAndDate(mediaType, dims, date)) {
                    for (FileMetadata metadata : media.getMetadataStream()
                            .filter(fm -> fm.isIgnored() != Boolean.TRUE && fm.shouldRead()).toList()) {
                        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                            mediaService.updateReadableStateAndHashes(
                                    new MediaUpdateContext<>(media, null, getUrlResolver(), httpClient, null, false,
                                            false),
                                    metadata);
                            ongoingUpdateMedia(start, count + localCount++);
                        }
                    }
                }
            }
        }
        return localCount;
    }

    private static List<String> fetchFiles(String browseUrl, String textToFind) throws IOException {
        return Jsoup.connect(browseUrl).header("Accept", "text/html").timeout(30_000).get().getElementsByTag("a")
                .stream().filter(a -> a.nextSibling() == null || !a.nextSibling().toString().endsWith(" 0"))
                .map(e -> e.attr("href")).filter(href -> href.contains(textToFind)).sorted().toList();
    }

    private void updateKeywords(LocalDate date, NasaMediaType mediaType, List<NasaSdoKeywords> aiaKeywords,
            List<NasaSdoKeywords> hmiKeywords, ImageDimensions dims) {
        if (isNotEmpty(aiaKeywords) || isNotEmpty(hmiKeywords)) {
            for (NasaSdoMedia media : sdoRepository.findByMediaTypeAndDimensionsAndDateAndFsnIsNull(mediaType, dims,
                    date)) {
                SdcStatements statements = updateKeywords(aiaKeywords, hmiKeywords, media);
                for (String uploadFileName : media.getUniqueMetadata().getCommonsFileNames()) {
                    LOGGER.info("Updating SDC keywords for {} : {}", media, uploadFileName);
                    try {
                        commonsService.editStructuredDataContent(uploadFileName.replace('_', ' '), null, statements);
                    } catch (IOException | MediaWikiApiErrorException e) {
                        LOGGER.error("Failed to update SDC keywords: {}", e.getMessage(), e);
                        GlitchTip.capture(e);
                    }
                }
            }
        }
    }

    private SdcStatements updateKeywords(List<NasaSdoKeywords> aiaKeywords, List<NasaSdoKeywords> hmiKeywords,
            NasaSdoMedia media) {
        NasaSdoDataType type = media.getDataType();
        SdcStatements statements = new SdcStatements();
        if (type.getInstrument() != EVE) {
            List<NasaSdoKeywords> keywords = AIA == type.getInstrument() ? aiaKeywords : hmiKeywords;
            if (isNotEmpty(keywords)) {
                List<NasaSdoKeywords> matches = filterKeywords(keywords, type.getWavelength(),
                        media.getCreationDateTime());
                if (matches.isEmpty()) {
                    LOGGER.warn("No keyword match found for {}", media);
                } else if (matches.size() > 1) {
                    LOGGER.warn("Several keyword matches found for {} : {}", media, matches);
                } else {
                    LOGGER.info("Found keyword match for {} : {}", media, matches);
                    media.setKeywords(matches.get(0));
                    addSdcKeywordsStatements(saveMedia(media).getKeywords(), statements);
                }
            }
        }
        return statements;
    }

    private static List<NasaSdoKeywords> filterKeywords(List<NasaSdoKeywords> keywords, int wavelength,
            ZonedDateTime time) {
        return keywords.stream()
                .filter(kw -> (kw.getWavelength() == null || kw.getWavelength() == wavelength) && kw.gettObs() != null
                        && kw.gettObs().getHour() == time.getHour() && kw.gettObs().getMinute() == time.getMinute()
                        && kw.gettObs().getSecond() == time.getSecond())
                .toList();
    }

    private NasaSdoMedia updateMedia(CompositeMediaId id, Temporal temporal, ImageDimensions dimensions, URL url,
            NasaMediaType mediaType, NasaSdoDataType dataType, List<NasaSdoMedia> uploadedMedia)
            throws IOException {
        boolean save = false;
        NasaSdoMedia media = null;
        Optional<NasaSdoMedia> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = newMedia(id, temporal, dimensions, url, mediaType, dataType);
            save = true;
        }
        return doUpdateMedia(media, save, uploadedMedia);
    }

    private NasaSdoMedia newMedia(CompositeMediaId id, Temporal temporal, ImageDimensions dimensions, URL url,
            NasaMediaType mediaType, NasaSdoDataType dataType) {
        NasaSdoMedia media;
        media = new NasaSdoMedia();
        media.setId(id);
        media.setTitle(id.getMediaId());
        if (temporal instanceof ZonedDateTime date) {
            media.setCreationDateTime(date);
            media.setPublicationDateTime(date);
        } else if (temporal instanceof LocalDate date) {
            media.setCreationDate(date);
            media.setPublicationDate(date);
        }
        media.setDataType(dataType);
        if (dimensions.getWidth() == 4096) {
            media.setThumbnailUrl(newURL(url.toExternalForm().replace("_4096_", "_1024_")));
        }
        addMetadata(media, url, fm -> fm.setImageDimensions(dimensions));
        media.setMediaType(mediaType);
        return media;
    }

    private NasaSdoMedia doUpdateMedia(NasaSdoMedia media, boolean shouldSave, List<NasaSdoMedia> uploadedMedia)
            throws IOException {
        boolean save = shouldSave;
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (shouldUploadAuto(media, false)) {
            try {
                media = saveMedia(upload(save ? saveMedia(media) : media, true, false).getLeft());
                uploadedMedia.add(media);
                save = false;
            } catch (UploadException e) {
                LOGGER.error("Unable to upload {}", media, e);
                GlitchTip.capture(e);
            }
        }
        if (save) {
            media = saveMedia(media);
        }
        return media;
    }

    @Override
    public NasaSdoMedia getById(String id) throws ImageNotFoundException {
        return getMediaWithUpdatedKeywords(super.getById(id));
    }

    @Override
    protected NasaSdoMedia findBySha1OrThrow(String sha1, boolean throwIfNotFound) throws TooManyResultsException {
        return getMediaWithUpdatedKeywords(super.findBySha1OrThrow(sha1, throwIfNotFound));
    }

    private NasaSdoMedia getMediaWithUpdatedKeywords(NasaSdoMedia media) {
        try {
            updateKeywords(media.getDataType().getInstrument() == NasaSdoInstrument.AIA
                    ? fetchAiaKeywords(media.getCreationDate())
                    : null, null, media); // HMI keywords not fetched, see other call
            return media;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<String> findCategories(NasaSdoMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (media.isVideo()) {
            result.add("Videos from the Solar Dynamics Observatory");
        }
        return result;
    }

    @Override
    protected Pair<String, Map<String, String>> getWikiFileDesc(NasaSdoMedia media, FileMetadata metadata)
            throws IOException {
        return Pair.of("{{NASA SDO|instrument=" + media.getDataType().getInstrument() + "|band="
                + media.getDataType().name().replace("_", "") + "|type=" + metadata.getFileExtensionOnCommons() + "|id="
                + media.getId().getMediaId() + "}}", Map.of());
    }

    @Override
    public Set<String> findLicenceTemplates(NasaSdoMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaSdoMedia refresh(NasaSdoMedia media) throws IOException {
        FileMetadata fm = media.getMetadata().iterator().next();
        return media.copyDataFrom(newMedia(media.getId(),
                Optional.<Temporal>ofNullable(media.getCreationDateTime()).orElse(media.getCreationDate()),
                fm.getImageDimensions(), fm.getAssetUrl(), media.getMediaType(), media.getDataType()));
    }

    @Override
    protected Set<String> getEmojis(NasaSdoMedia uploadedMedia) {
        return Set.of(Emojis.SUN);
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaSdoMedia uploadedMedia) {
        return Set.of("@NASASun");
    }
}
