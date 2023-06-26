package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Service
public class NasaPhotojournalService
        extends AbstractOrgService<NasaPhotojournalMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaPhotojournalService.class);

    static final Pattern ANIMATION_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://[^\"]+\\.(?:gif|mp4))\".*");

    static final Pattern QTVR_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://[^\"]+\\.mov)\".*");

    static final Pattern FIGURE_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://[^\"]+/figures/[^\"]+\\.(?:jpg|png))\".*");

    static final Pattern ACQ_PATTERN = Pattern.compile(
            ".*acquired ((?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, [1-2]\\d{3}).*");

    static final Pattern LOCATION_PATTERN = Pattern.compile(
            ".*located at (\\d+\\.?\\d*) degrees (north|south), (\\d+\\.?\\d*) degrees (west|east).*");

    private static final DateTimeFormatter ACQ_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    @Autowired
    private NasaPhotojournalService self;

    @Value("${nasa.photojournal.solr.retries}")
    private int solrRetries;

    @Value("${nasa.photojournal.solr.page:50}")
    private int solrPage;

    @Value("${nasa.photojournal.solr.host}")
    private String host;

    @Value("${nasa.photojournal.geohack.globes}")
    private Set<String> globes;

    private Map<String, Map<String, String>> nasaInstruments;
    private Map<String, Map<String, String>> nasaMissions;
    private Map<String, String> nasaKeywords;

    @Autowired
    public NasaPhotojournalService(NasaPhotojournalMediaRepository repository) {
        super(repository, "nasa.photojournal");
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        nasaInstruments = loadCsvMapMapping("nasa.instruments.csv");
        nasaMissions = loadCsvMapMapping("nasa.missions.csv");
        nasaKeywords = loadCsvMapping("nasa.keywords.csv");
    }

    private SolrQuery buildSolrQuery(int start) {
        return new SolrQuery("*:*").setSort("publication-date", ORDER.desc).setRows(solrPage).setStart(start);
    }

    private QueryResponse queryWithRetries(SolrQuery query) throws IOException {
        for (int i = 0; i < solrRetries; i++) {
            try {
                return solrClient(host).query(query);
            } catch (RemoteSolrException | SolrServerException e) {
                LOGGER.warn("{}", e.getMessage());
                LOGGER.warn("Retry {} on {}", i, solrRetries);
                LOGGER.debug("Retry {} on {}", i, solrRetries, e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOGGER.error(e.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
        LOGGER.error("Exhausted {} retries", solrRetries);
        return null;
    }

    @Override
    public String getName() {
        return "NASA (Photojournal)";
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        processResponse(queryWithRetries(buildSolrQuery(0)));
    }

    protected List<NasaPhotojournalMedia> processResponse(QueryResponse response) throws IOException, UploadException {
        List<NasaPhotojournalMedia> result = new ArrayList<>();
        if (response != null) {
            final long total = response.getResults().getNumFound();
            LOGGER.info("Found {} documents", total);
            result.addAll(processDocuments(response.getResults()));
            for (int start = solrPage; start < total; start += solrPage) {
                response = queryWithRetries(buildSolrQuery(start));
                if (response != null) {
                    result.addAll(processDocuments(response.getResults()));
                }
            }
        }
        return result;
    }

    private List<NasaPhotojournalMedia> processDocuments(SolrDocumentList documents)
            throws IOException, UploadException {
        List<NasaPhotojournalMedia> result = new ArrayList<>();
        for (SolrDocument document : documents) {
            result.add(self.processMedia((String) document.getFirstValue("id"), document));
        }
        return result;
    }

    @Transactional
    public NasaPhotojournalMedia processMedia(String id, SolrDocument document) throws IOException, UploadException {
        Optional<NasaPhotojournalMedia> mediaInRepo = repository.findById(id);
        NasaPhotojournalMedia media;
        boolean save = false;
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
            // To remove after all previous files have been correctly identified
            save |= detectFigures(media);
        } else {
            media = solrDocumentToMedia(document);
            save = true;
        }
        save |= doCommonUpdate(media);
        save |= ignoreNonFreeFiles(media);
        if (shouldUploadAuto(media, false)) {
            media = saveMedia(upload(save ? saveMedia(media) : media, true, false).getLeft());
            save = false;
        }
        return save ? saveMedia(media) : media;
    }

    private boolean ignoreNonFreeFiles(NasaPhotojournalMedia media) {
        String credit = media.getCredit();
        return !credit.contains("NASA") && !credit.contains("JPL") && !credit.contains("Jet Propulsion Laboratory")
                && !credit.contains("USSF") && ignoreFile(media, "Non-free content");
    }

    private NasaPhotojournalMedia solrDocumentToMedia(SolrDocument doc) {
        sanityChecks(doc);
        NasaPhotojournalMedia media = new NasaPhotojournalMedia();
        String caption = (String) doc.getFirstValue("original-caption");
        media.setDescription(caption);
        media.setId((String) doc.getFirstValue("id"));
        media.setNasaId((String) doc.getFirstValue("nasa-id"));
        media.setDate(((Date) doc.getFirstValue("publication-date")).toInstant().atZone(ZoneOffset.UTC));
        media.setTarget((String) doc.getFirstValue("target"));
        media.setMission((String) doc.getFirstValue("mission"));
        media.setSpacecraft((String) doc.getFirstValue("spacecraft"));
        media.setInstrument((String) doc.getFirstValue("instrument"));
        media.setProducer((String) doc.getFirstValue("producer"));
        media.setThumbnailUrl(newURL((String) doc.getFirstValue("browse-url")));
        media.setTitle((String) doc.getFieldValue("image-title"));
        media.setBig("YES".equals(doc.getFirstValue("big-flag")));
        media.setCredit((String) doc.getFirstValue("credit"));
        media.setLegend((String) doc.getFirstValue("alt-tag"));
        ImageDimensions dims = new ImageDimensions(getInt(doc, "x-dim"), getInt(doc, "y-dim"));
        addMetadata(media, (String) doc.getFirstValue("full-res-jpeg"), m -> m.setImageDimensions(dims));
        addMetadata(media, (String) doc.getFirstValue("full-res-tiff"), m -> m.setImageDimensions(dims));
        Collection<Object> keywords = doc.getFieldValues("keywords");
        if (keywords != null) {
            media.setKeywords(keywords.stream().map(String.class::cast).collect(toSet()));
            boolean isAnimation = media.getKeywords().contains("animation");
            boolean isQtvr = media.getKeywords().contains("qtvr");
            if (isAnimation || isQtvr) {
                Matcher m = (isAnimation ? ANIMATION_PATTERN : QTVR_PATTERN).matcher(caption);
                if (m.matches()) {
                    addMetadata(media, newURL(m.group(1)), null);
                }
            }
        }
        detectFigures(media);
        return media;
    }

    private static Integer getInt(SolrDocument doc, String key) {
        return (Integer) doc.getFirstValue(key);
    }

    private boolean detectFigures(NasaPhotojournalMedia media) {
        String caption = media.getDescription();
        if (caption.contains("<img ")) {
            Matcher m = FIGURE_PATTERN.matcher(caption);
            if (m.matches()) {
                String url = m.group(1);
                if (!media.containsMetadata(url)) {
                    addMetadata(media, url, null);
                    return true;
                }
            }
        }
        return false;
    }

    private void sanityChecks(SolrDocument doc) {
        String catalogUrl = (String) doc.getFirstValue("catalog-url");
        if (!"IMAGE".equals(doc.getFirstValue("data-type")) || !"image".equals(doc.getFieldValue("image-type"))) {
            problem(catalogUrl, "Not an image: " + doc);
        }
        if ("YES".equals(doc.getFirstValue("proprietary"))) {
            problem(catalogUrl, "Proprietary image: " + doc);
        }
        if (!"YES".equals(doc.getFirstValue("ready-flag"))) {
            throw new IllegalArgumentException("Image not ready: " + doc);
        }
        if (!"YES".equals(doc.getFirstValue("release-flag"))) {
            throw new IllegalArgumentException("Image not released: " + doc);
        }
    }

    @Override
    public URL getSourceUrl(NasaPhotojournalMedia media, FileMetadata metadata) {
        return newURL("https://photojournal.jpl.nasa.gov/catalog/" + media.getId());
    }

    @Override
    protected final Pair<String, Map<String, String>> getWikiFileDesc(NasaPhotojournalMedia media, FileMetadata metadata)
            throws MalformedURLException {
        // https://commons.wikimedia.org/wiki/Template:NASA_Photojournal/attribution/mission
        String lang = getLanguage(media);
        String desc = getDescription(media);
        StringBuilder sb = new StringBuilder("{{NASA Photojournal\n| catalog = ").append(media.getId())
                .append("\n| image= ").append(media.isImage()).append("\n| video= ").append(media.isVideo())
                .append("\n| animation= ").append("gif".equals(metadata.getFileExtension()))
                .append("\n| mission= ").append(media.getMission())
                .append("\n| instrument= ").append(media.getInstrument()).append("\n| caption = ").append("{{")
                .append(lang).append("|1=").append(CommonsService.formatWikiCode(desc))
                .append("}}\n| credit= ").append(media.getCredit());
        getUploadDate(media).ifPresent(s -> sb.append("\n| addition_date = ").append(toIso8601(s)));
        sb.append("\n| creation_date = ");
        getCreationDate(media).ifPresent(sb::append);
        if (globes.contains(media.getTarget())) {
            sb.append("\n| globe= ").append(media.getTarget());
        }
        getLocation(media)
                .ifPresent(p -> sb.append("\n| lat= ").append(p.getX()).append("\n| long= ").append(p.getY()));
        appendWikiOtherVersions(sb, media, metadata, "gallery");
        if (List.of("gif", "mp4").contains(metadata.getFileExtension())) {
            sb.append("\n| link= ").append(metadata.getAssetUrl());
        }
        sb.append("\n}}");
        return Pair.of(sb.toString(), Map.of(lang, desc));
    }

    protected final Optional<Point> getLocation(NasaPhotojournalMedia media) {
        Matcher m = LOCATION_PATTERN.matcher(media.getDescription());
        return m.matches()
                ? Optional.of(new Point(
                        parseDouble(m.group(1)) * ("north".equalsIgnoreCase(m.group(2)) ? 1 : -1),
                        parseDouble(m.group(3)) * ("east".equalsIgnoreCase(m.group(4)) ? 1 : -1)))
                : Optional.empty();
    }

    @Override
    protected final Optional<Temporal> getCreationDate(NasaPhotojournalMedia media) {
        Matcher m = ACQ_PATTERN.matcher(media.getDescription());
        return m.matches() ? Optional.of(ACQ_DATE_FORMAT.parse(m.group(1), LocalDate::from)) : Optional.empty();
    }

    @Override
    protected final Optional<Temporal> getUploadDate(NasaPhotojournalMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    public Set<String> findCategories(NasaPhotojournalMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.add("NASA Photojournal entries from " + media.getYear() + '|' + media.getId());
        if (media.getKeywords().contains("anaglyph")) {
            result.add("Moon".equalsIgnoreCase(media.getTarget()) ? "Anaglyphs of the Moon" : "Anaglyphs");
        }
        if (media.getKeywords().contains("animation") && "gif".equals(metadata.getFileExtension())) {
            result.add("Mars".equalsIgnoreCase(media.getTarget()) ? "Animated GIF of Mars" : "Animated GIF files");
        }
        if (media.getKeywords().contains("qtvr") && "mov".equals(metadata.getFileExtension())) {
            // Not sure what to do about these files
        }
        if (media.getKeywords().contains("artist")) {
            result.add("Art from NASA");
        }
        result.addAll(media.getKeywords().stream().map(nasaKeywords::get).filter(Objects::nonNull).toList());
        getCategoryFromMapping(media.getInstrument(), "instrument", nasaInstruments).ifPresent(result::add);
        getCategoryFromMapping(media.getMission(), "mission", nasaMissions).ifPresent(result::add);
        if ("Mars".equalsIgnoreCase(media.getTarget())) {
            replace(result, "2001 Mars Odyssey", "Photos of Mars by 2001 Mars Odyssey");
            replace(result, "Photos by THEMIS", "Photos of Mars by THEMIS");
        }
        return result;
    }

    private static Optional<String> getCategoryFromMapping(String value, String type,
            Map<String, Map<String, String>> mappings) {
        if (value != null) {
            Map<String, String> map = mappings.get(value.replace('\n', ' ').trim());
            if (map != null) {
                String cat = map.get("Commons categories");
                if (isBlank(cat)) {
                    LOGGER.warn("No category found for NASA {} {}", type, value);
                } else {
                    return Optional.of(cat);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<String> findLicenceTemplates(NasaPhotojournalMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("JPL Image Copyright");
        return result;
    }

    @Override
    protected NasaPhotojournalMedia refresh(NasaPhotojournalMedia media) throws IOException {
        QueryResponse response = queryWithRetries(new SolrQuery(media.getId()));
        return response != null && response.getResults().getNumFound() == 1
                ? media.copyDataFrom(solrDocumentToMedia(response.getResults().get(0)))
                : media;
    }

    @Override
    protected String getAuthor(NasaPhotojournalMedia media) throws MalformedURLException {
        return media.getCredit();
    }

    @Override
    protected Class<NasaPhotojournalMedia> getMediaClass() {
        return NasaPhotojournalMedia.class;
    }

    @Override
    protected String getMediaId(String id) {
        return id;
    }

    @Override
    protected Map<String, String> getLegends(NasaPhotojournalMedia media, Map<String, String> descriptions) {
        Map<String, String> result = new TreeMap<>(super.getLegends(media, descriptions));
        if (isNotBlank(media.getLegend())) {
            result.put("en", media.getLegend());
        }
        String legend = result.get("en");
        if (legend != null && legend.startsWith("<")) {
            if (legend.contains("Today's")) {
                result.put("en", legend = legend.substring(legend.indexOf("Today's")));
            }
            if (legend.contains("This VIS ")) {
                result.put("en", legend = legend.substring(legend.indexOf("This VIS ")));
            }
        }
        return result;
    }

    private static SolrClient solrClient(String host) {
        return new HttpSolrClient.Builder(host).withResponseParser(new XMLResponseWithoutContentTypeParser()).build();
    }

    protected static final class XMLResponseWithoutContentTypeParser extends XMLResponseParser {
        @Override
        public String getContentType() {
            // Photojournal nginx returns text/plain instead of application/json
            return null;
        }
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaPhotojournalMedia uploadedMedia) {
        return Set.of("@NASAJPL");
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(NasaPhotojournalMedia media,
            FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        wikidataStatementMapping(media.getInstrument(), nasaInstruments, "P4082", result); // Taken with instrument
        wikidataStatementMapping(media.getSpacecraft(), nasaMissions, "P170", result); // Created by mission
        return result;
    }
}
