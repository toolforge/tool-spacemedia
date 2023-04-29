package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Service
public class NasaPhotojournalService
        extends
        AbstractFullResExtraAgencyService<NasaPhotojournalMedia, String, ZonedDateTime, NasaPhotojournalMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaPhotojournalService.class);

    static final Pattern ANIMATION_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://.+\\.(?:gif|mp4))\".*");

    static final Pattern QTVR_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://.+\\.mov)\".*");

    static final Pattern FIGURE_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://.+/figures/.+\\.png)\".*");

    static final Pattern ACQ_PATTERN = Pattern.compile(
            ".*acquired ((?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, [1-2]\\d{3}).*");

    static final Pattern LOCATION_PATTERN = Pattern.compile(
            ".*located at (\\d+\\.?\\d*) degrees (north|south), (\\d+\\.?\\d*) degrees (west|east).*");

    private static final DateTimeFormatter ACQ_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    @Value("${nasa.photojournal.solr.retries}")
    private int solrRetries;

    @Value("${nasa.photojournal.solr.page:50}")
    private int solrPage;

    @Value("${nasa.photojournal.solr.host}")
    private String host;

    @Value("${nasa.photojournal.geohack.globes}")
    private Set<String> globes;

    private Map<String, String> nasaKeywords;
    private Map<String, String> nasaMissions;

    @Autowired
    public NasaPhotojournalService(NasaPhotojournalMediaRepository repository) {
        super(repository, "nasa.photojournal");
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        nasaKeywords = loadCsvMapping("nasa.keywords.csv");
        nasaMissions = loadCsvMapping("nasa.missions.csv");
    }

    private SolrQuery buildSolrQuery(int start) {
        SolrQuery query = new SolrQuery("*:*");
        query.setSort("publication-date", ORDER.desc);
        query.setRows(solrPage);
        query.setStart(start);
        return query;
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
        QueryResponse response = queryWithRetries(buildSolrQuery(0));
        if (response != null) {
            final long total = response.getResults().getNumFound();
            LOGGER.info("Found {} documents", total);
            processDocuments(response.getResults());
            for (int start = solrPage; start < total; start += solrPage) {
                response = queryWithRetries(buildSolrQuery(start));
                if (response != null) {
                    processDocuments(response.getResults());
                }
            }
        }
    }

    private void processDocuments(SolrDocumentList documents) throws IOException {
        for (SolrDocument document : documents) {
            processMedia((String) document.getFirstValue("id"), document);
        }
    }

    private NasaPhotojournalMedia processMedia(String id, SolrDocument document) throws IOException {
        Optional<NasaPhotojournalMedia> mediaInRepo = repository.findById(id);
        NasaPhotojournalMedia media;
        boolean save = false;
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
            // To remove after all previous files have been correctly identified
            if (detectFigures(media)) {
                save = true;
            }
        } else {
            media = solrDocumentToMedia(document);
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (save) {
            media = saveMedia(media);
        }
        return media;
    }

    private NasaPhotojournalMedia solrDocumentToMedia(SolrDocument doc) throws MalformedURLException {
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
        media.setThumbnailUrl(new URL((String) doc.getFirstValue("browse-url")));
        media.setTitle((String) doc.getFieldValue("image-title"));
        media.setBig("YES".equals(doc.getFirstValue("big-flag")));
        media.setCredit((String) doc.getFirstValue("credit"));
        media.getMetadata().setAssetUrl(new URL((String) doc.getFirstValue("full-res-jpeg")));
        media.getFullResMetadata().setAssetUrl(new URL((String) doc.getFirstValue("full-res-tiff")));
        Collection<Object> keywords = doc.getFieldValues("keywords");
        if (keywords != null) {
            media.setKeywords(keywords.stream().map(String.class::cast).collect(toSet()));
            boolean isAnimation = media.getKeywords().contains("animation");
            boolean isQtvr = media.getKeywords().contains("qtvr");
            if (isAnimation || isQtvr) {
                Matcher m = (isAnimation ? ANIMATION_PATTERN : QTVR_PATTERN).matcher(caption);
                if (m.matches()) {
                    media.getExtraMetadata().setAssetUrl(new URL(m.group(1)));
                }
            }
        }
        detectFigures(media);
        return media;
    }

    private boolean detectFigures(NasaPhotojournalMedia media) throws MalformedURLException {
        String caption = media.getDescription();
        if (media.getExtraMetadata().getAssetUrl() == null && caption.contains("<img ")) {
            Matcher m = FIGURE_PATTERN.matcher(caption);
            if (m.matches()) {
                media.getExtraMetadata().setAssetUrl(new URL(m.group(1)));
                return true;
            }
        }
        return false;
    }

    private void sanityChecks(SolrDocument doc) throws MalformedURLException {
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
    public URL getSourceUrl(NasaPhotojournalMedia media) throws MalformedURLException {
        return new URL("https://photojournal.jpl.nasa.gov/catalog/" + media.getId());
    }

    @Override
    protected final String getWikiFileDesc(NasaPhotojournalMedia media, Metadata metadata)
            throws MalformedURLException {
        StringBuilder sb = new StringBuilder("{{NASA Photojournal\n| catalog = ").append(media.getId())
                .append("\n| image= ").append(media.isImage()).append("\n| video= ").append(media.isVideo())
                .append("\n| animation= ").append("gif".equals(metadata.getFileExtension()))
                .append("\n| mission= ").append(media.getMission())
                .append("\n| instrument= ").append(media.getInstrument()).append("\n| caption = ").append("{{")
                .append(getLanguage(media)).append("|1=").append(CommonsService.formatWikiCode(getDescription(media)))
                .append("}}\n| credit= ").append(media.getCredit());
        getUploadDate(media).ifPresent(s -> sb.append("\n| addition_date = ").append(toIso8601(s)));
        getCreationDate(media).ifPresent(s -> sb.append("\n| creation_date = ").append(s));
        if (globes.contains(media.getTarget())) {
            sb.append("\n| globe= ").append(media.getTarget());
        }
        getLocation(media)
                .ifPresent(p -> sb.append("\n| lat= ").append(p.getX()).append("\n| long= ").append(p.getY()));
        getOtherVersions(media, metadata).ifPresent(s -> sb.append("\n| gallery = ").append(s));
        if (List.of("gif", "mp4").contains(metadata.getFileExtension())) {
            sb.append("\n| link= ").append(metadata.getAssetUrl());
        }
        sb.append("\n}}");
        return sb.toString();
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
    public Set<String> findCategories(NasaPhotojournalMedia media, Metadata metadata, boolean includeHidden) {
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
        if (media.getMission() != null) {
            String cat = nasaMissions.get(media.getMission().trim());
            if (cat != null) {
                result.add(cat);
            } else {
                LOGGER.error("No category found for NASA mission {}", media.getMission());
            }
        }
        if ("Mars".equalsIgnoreCase(media.getTarget())) {
            if (result.contains("2001 Mars Odyssey")) {
                result.remove("2001 Mars Odyssey");
                result.add("Photos of Mars by 2001 Mars Odyssey");
            }
            if (result.contains("Photos of Mars by 2001 Mars Odyssey")
                    && "THEMIS".equalsIgnoreCase(media.getInstrument())) {
                result.remove("Photos of Mars by 2001 Mars Odyssey");
                result.add("Photos of Mars by THEMIS");
            }
        }
        return result;
    }

    @Override
    public Set<String> findTemplates(NasaPhotojournalMedia media) {
        Set<String> result = super.findTemplates(media);
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

    private static SolrClient solrClient(String host) {
        return new HttpSolrClient.Builder(host).withResponseParser(new XMLResponseWithoutContentTypeParser()).build();
    }

    private static class XMLResponseWithoutContentTypeParser extends XMLResponseParser {
        @Override
        public String getContentType() {
            // Photojournal nginx returns text/plain instead of application/json
            return null;
        }
    }

    @Override
    protected Set<String> getEmojis(NasaPhotojournalMedia uploadedMedia) {
        return AbstractSocialMediaService.getEmojis(uploadedMedia.getKeywords());
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaPhotojournalMedia uploadedMedia) {
        return Set.of("@NASAJPL");
    }
}
