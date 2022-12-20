package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Service
public class NasaPhotojournalService
        extends
        AbstractFullResAgencyService<NasaPhotojournalMedia, String, ZonedDateTime, NasaPhotojournalMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaPhotojournalService.class);

    private static final Pattern ANIMATION_PATTERN = Pattern.compile(
            ".*<a href=\"(http[^\"]+)\" target=\"new\"><b>Click here for animation</b></a>.*");

    @Value("${nasa.photojournal.solr.retries}")
    private int solrRetries;

    @Value("${nasa.photojournal.solr.page:50}")
    private int solrPage;

    @Value("${nasa.photojournal.solr.host}")
    private String host;

    @Autowired
    public NasaPhotojournalService(NasaPhotojournalMediaRepository repository) {
        super(repository, "nasa.photojournal");
    }

    private SolrQuery buildSolrQuery(int start) {
        SolrQuery query = new SolrQuery("*:*");
        // Fields
        query.addField("alt-tag"); // Ex: NASA's Perseverance Mars rover used [...] on June 6, 2022.
        query.addField("big-flag"); // Ex: YES
        query.addField("browse-url"); // Ex: https://photojournal.jpl.nasa.gov/browse/PIA25672.jpg
        query.addField("catalog-url"); // Ex: https://photojournal.jpl.nasa.gov/catalog/PIA25672
        query.addField("category"); // Ex: Mars
        query.addField("credit"); // Ex: NASA/JPL-Caltech/ASU/MSSS
        query.addField("data-type"); // Ex: IMAGE
        query.addField("full-res-jpeg"); // Ex: https://photojournal.jpl.nasa.gov/jpeg/PIA25672.jpg
        query.addField("full-res-tiff"); // Ex: https://photojournal.jpl.nasa.gov/tiff/PIA25672.tif
        query.addField("id"); // Ex: PIA25672
        query.addField("image-title"); // Ex: Perseverance's Mastcam-Z Views Hogwallow Flats
        query.addField("image-type"); // Ex: image
        query.addField("instrument"); // Ex: Mastcam-Z
        query.addField("instrument-host"); // Ex: Perseverance
        query.addField("investigation"); // Ex: Mars 2020 Rover
        query.addField("keywords"); // Ex: animation
        query.addField("mission"); // Ex: Mars 2020 Rover
        //query.addField("mobile-browse"); // Ex: https://photojournal.jpl.nasa.gov/ipbrowse/PIA25672_ip.jpg
        //query.addField("mobile-thumbnail"); // Ex: https://photojournal.jpl.nasa.gov/ipthumbs/PIA25672_ipthumb.jpg
        query.addField("modification-time"); // Ex: 2022-12-16T00:40:37.000000Z
        query.addField("nasa-id"); // Ex: JPL-20221215-PIA25672-M2020
        query.addField("original-caption"); // Ex: <p><center>...</p>
        // query.addField("pinumber"); // Ex: PIA25672
        query.addField("producer"); // Ex: Malin Space Science Systems
        query.addField("proprietary"); // Ex: NO
        query.addField("publication-date"); // Ex: 2022-12-15T22:43:37Z
        query.addField("ready-flag"); // Ex: YES
        // query.addField("record-creation-time"); // Ex: 2022-12-16T00:47:52.000000Z
        // query.addField("record-modification-time"); // Ex: 2022-12-16T02:00:21Z
        query.addField("release-flag"); // Ex: YES
        query.addField("spacecraft"); // Ex: Perseverance
        query.addField("target"); // Ex: Mars
        query.addField("x-dim"); // Ex: 40562
        query.addField("y-dim"); // Ex: 5548
        query.addField("z-dim"); // Ex: 3

        // Sorting, paging
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
            if (media.getKeywords().contains("animation")) {
                Matcher m = ANIMATION_PATTERN.matcher(caption);
                if (m.matches()) {
                    media.getExtraMetadata().setAssetUrl(new URL(m.group(1)));
                }
            }
        }
        return media;
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
    protected NasaPhotojournalMedia refresh(NasaPhotojournalMedia media) throws IOException {
        // TODO Auto-generated method stub
        return null;
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
}
