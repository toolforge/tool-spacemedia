package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoInstrument.AIA;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaSdoService extends AbstractOrgService<NasaSdoMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaSdoService.class);

    private static final String BASE_URL = "https://sdo.gsfc.nasa.gov";
    private static final String BASE_URL_IMG = BASE_URL + "/assets/img";
    private static final String JSOC_BASE_URL = "http://jsoc.stanford.edu/data";

    static final DateTimeFormatter IMG_NAME_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    static final DateTimeFormatter AIA_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSX");
    static final DateTimeFormatter HMI_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH:mm:ss.SSX");

    @Autowired
    private NasaSdoMediaRepository sdoRepository;

    public NasaSdoService(NasaSdoMediaRepository repository) {
        super(repository, "nasa.sdo", Set.of("sdo"));
    }

    @Override
    protected boolean isNASA(NasaSdoMedia media) {
        return true;
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
    public URL getSourceUrl(NasaSdoMedia media, FileMetadata metadata) {
        return metadata.getAssetUrl();
    }

    @Override
    protected String getAuthor(NasaSdoMedia media) {
        // https://sdo.gsfc.nasa.gov/data/rules.php
        return "Courtesy of NASA/SDO and the AIA, EVE, and HMI science teams.";
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(NasaSdoMedia media, FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        NasaSdoDataType dataType = media.getDataType();
        result.put("P31", Pair.of(switch (dataType) {
        case _HMID -> "Q5297355"; // dopplergram
        case _HMIB, _HMIBC -> "Q115801008"; // magnetogram
        case _HMII, _HMIIC, _HMIIF -> "Q119021644"; // intensitygram
        default -> metadata.isVideo() ? "Q98069877" : "Q125191"; // video or photograph
        }, null));
        result.put("P170", Pair.of("Q382494", null)); // Created by SDO
        result.put("P180", Pair.of("Q525", null)); // Depicts the Sun
        result.put("P1071", Pair.of("Q472251", null)); // Created in geosynchronous orbit
        result.put("P2079", Pair.of("Q725252", null)); // Satellite imagery
        result.put("P2808", Pair.of(Pair.of(dataType.getWavelength(), "Q81454"), null)); // Wavelength in ångström (Å)
        result.put("P4082", Pair.of(dataType.getInstrument().getQid(), null)); // Taken with SDO instrument
        addSdcKeywordsStatements(media.getKeywords(), result);
        return result;
    }

    private static void addSdcKeywordsStatements(NasaSdoKeywords keywords,
            Map<String, Pair<Object, Map<String, Object>>> result) {
        Optional.ofNullable(keywords).map(NasaSdoKeywords::getExpTime) // Exposure time in seconds
                .ifPresent(exp -> result.put("P6757", Pair.of(Pair.of(exp, "Q11574"), null)));
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaSdoMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        for (LocalDate date = LocalDate.now(); date.getYear() >= 2010
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
                CloseableHttpResponse response = httpclient.execute(newHttpGet(keywordsUrl));
                InputStream in = response.getEntity().getContent()) {
            if (response.getStatusLine().getStatusCode() >= 400) {
                LOGGER.warn("{} => {}", keywordsUrl, response.getStatusLine());
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
        return index >= 0 ? Optional.of(mapper.apply(line.substring(index, line.indexOf(' ', index))))
                : Optional.empty();
    }

    // First image of each day
    private int updateImages(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count) throws IOException {
        List<NasaSdoKeywords> aiaKeywords = sdoRepository.existsByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(
                NasaMediaType.image, NasaSdoDataType.values(AIA), date) ? fetchAiaKeywords(date) : List.of();
        // HMI keywords are not fetched as it is not clear how to match level 0 keywords
        // with HMI 15 minutes catalog
        return updateImagesOrVideos(dateStringPath, date, 4096, NasaMediaType.image, "browse", "jpg", aiaKeywords,
                null, uploadedMedia, start, count, NasaSdoService::parseImageDateTime);
    }

    static ZonedDateTime parseImageDateTime(String filename) {
        return LocalDateTime.parse(filename.substring(0, 15), IMG_NAME_DATE_TIME_FORMAT).atZone(ZoneId.of("UTC"));
    }

    // Daily movies
    private int updateVideos(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count) throws IOException {
        // TODO other mpeg/mp4 videos at JSOC
        // http://jsoc.stanford.edu/HMI/hmiimage.html
        // http://jsoc.stanford.edu/data/hmi/images/2023/05/01/Ic_flat_2d.mpg
        // http://jsoc.stanford.edu/data/hmi/images/2023/05/01/M_2d.mpg
        // http://jsoc.stanford.edu/data/hmi/images/2023/05/01/M_color_2d.mpg
        return updateImagesOrVideos(dateStringPath, date, 1024, NasaMediaType.video, "dailymov", "ogv", null, null,
                uploadedMedia, start, count, NasaSdoService::parseMovieDateTime);
    }

    static ZonedDateTime parseMovieDateTime(String filename) {
        return LocalDate.parse(filename.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay(ZoneId.of("UTC"));
    }

    private int updateImagesOrVideos(String dateStringPath, LocalDate date, int dim, NasaMediaType mediaType,
            String browse, String ext, List<NasaSdoKeywords> aiaKeywords, List<NasaSdoKeywords> hmiKeywords,
            List<NasaSdoMedia> uploadedMedia, LocalDateTime start, int count,
            Function<String, ZonedDateTime> dateTimeExtractor) throws IOException {
        ImageDimensions dims = new ImageDimensions(dim, dim);
        int localCount = 0;
        if (mediaType == NasaMediaType.image && date.getDayOfMonth() == 1) {
            LOGGER.info("Current update date: {} ...", date);
        }
        long imagesInDatabase = sdoRepository.countUploadedByMediaTypeAndDimensionsAndDate(mediaType, dims, date);
        int expectedCount = mediaType == NasaMediaType.video ? NasaSdoDataType.values().length - 1
                : NasaSdoDataType.values().length;
        if (imagesInDatabase < expectedCount) {
            String browseUrl = String.join("/", BASE_URL_IMG, browse, dateStringPath);
            LOGGER.info("Fetching {} ({}<{})", browseUrl, imagesInDatabase, expectedCount);
            try {
                List<String> files = Jsoup.connect(browseUrl).header("Accept", "text/html").timeout(30_000).get()
                        .getElementsByTag("a").stream().map(e -> e.attr("href"))
                        .filter(href -> href.contains("_" + dims.getWidth() + "_")).sorted().toList();
                for (NasaSdoDataType dataType : NasaSdoDataType.values()) {
                    Optional<String> opt = files.stream().filter(i -> i.endsWith(dataType.name() + '.' + ext))
                            .findFirst();
                    if (opt.isPresent()) {
                        String firstFile = opt.get();
                        updateMedia(new CompositeMediaId("sdo", firstFile.replace("." + ext, "")),
                                dateTimeExtractor.apply(firstFile), dims,
                                newURL(browseUrl + '/' + firstFile), mediaType, dataType, uploadedMedia);
                        ongoingUpdateMedia(start, count + localCount++);
                    }
                }
            } catch (HttpStatusException e) {
                // https://sdo.gsfc.nasa.gov/assets/img/browse/2016/12/25/ => HTTP 404
                LOGGER.error(e.getMessage(), e);
            }
        }
        updateKeywords(date, mediaType, aiaKeywords, hmiKeywords, dims);
        return localCount;
    }

    private void updateKeywords(LocalDate date, NasaMediaType mediaType, List<NasaSdoKeywords> aiaKeywords,
            List<NasaSdoKeywords> hmiKeywords, ImageDimensions dims) {
        if (isNotEmpty(aiaKeywords) || isNotEmpty(hmiKeywords)) {
            for (NasaSdoMedia media : sdoRepository.findByMediaTypeAndDimensionsAndDateAndFsnIsNull(mediaType, dims,
                    date)) {
                Map<String, Pair<Object, Map<String, Object>>> statements = updateKeywords(aiaKeywords, hmiKeywords,
                        media);
                for (String uploadFileName : media.getUniqueMetadata().getCommonsFileNames()) {
                    LOGGER.info("Updating SDC keywords for {} : {}", media, uploadFileName);
                    try {
                        commonsService.editStructuredDataContent(uploadFileName.replace('_', ' '), null, statements);
                    } catch (IOException | MediaWikiApiErrorException e) {
                        LOGGER.error("Failed to update SDC keywords: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }

    private Map<String, Pair<Object, Map<String, Object>>> updateKeywords(List<NasaSdoKeywords> aiaKeywords,
            List<NasaSdoKeywords> hmiKeywords, NasaSdoMedia media) {
        NasaSdoDataType type = media.getDataType();
        Map<String, Pair<Object, Map<String, Object>>> statements = new TreeMap<>();
        List<NasaSdoKeywords> keywords = AIA == type.getInstrument() ? aiaKeywords : hmiKeywords;
        if (isNotEmpty(keywords)) {
            List<NasaSdoKeywords> matches = filterKeywords(keywords, type.getWavelength(), media.getCreationDateTime());
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
        return statements;
    }

    private static List<NasaSdoKeywords> filterKeywords(List<NasaSdoKeywords> keywords, int wavelength,
            ZonedDateTime time) {
        return keywords.stream()
                .filter(kw -> (kw.getWavelength() == null || kw.getWavelength() == wavelength)
                        && kw.gettObs().getHour() == time.getHour() && kw.gettObs().getMinute() == time.getMinute()
                        && kw.gettObs().getSecond() == time.getSecond())
                .toList();
    }

    private NasaSdoMedia updateMedia(CompositeMediaId id, ZonedDateTime date, ImageDimensions dimensions, URL url,
            NasaMediaType mediaType, NasaSdoDataType dataType, List<NasaSdoMedia> uploadedMedia)
            throws IOException {
        boolean save = false;
        NasaSdoMedia media = null;
        Optional<NasaSdoMedia> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = new NasaSdoMedia();
            FileMetadata metadata = new FileMetadata(url);
            media.setId(id);
            media.setTitle(id.getMediaId());
            media.setCreationDateTime(date);
            media.setPublicationDateTime(date);
            media.setDataType(dataType);
            if (dimensions.getWidth() == 4096) {
                media.setThumbnailUrl(newURL(url.toExternalForm().replace("_4096_", "_1024_")));
            }
            metadata.setImageDimensions(dimensions);
            media.addMetadata(metadataRepository.save(metadata));
            media.setMediaType(mediaType);
            save = true;
        }
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
                + media.getDataType().name().replace("_", "") + "|type=" + metadata.getFileExtension() + "|id="
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
        throw new UnsupportedOperationException(); // TODO
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
