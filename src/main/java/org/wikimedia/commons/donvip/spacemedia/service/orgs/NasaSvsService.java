package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Comparator.comparingLong;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.NasaSvsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.NasaSvsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsCredits;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsMediaGroup;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsMediaItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsVizualisation;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NasaSvsService extends AbstractOrgService<NasaSvsMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaSvsService.class);

    private static final String API_SEARCH_ENDPOINT = "https://svs.gsfc.nasa.gov/api/search/?sort_by_0=DESC";
    private static final String API_VIZUAL_ENDPOINT = "https://svs.gsfc.nasa.gov/api/";

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    private NasaMappingService mappings;

    @Autowired
    public NasaSvsService(NasaSvsMediaRepository repository) {
        super(repository, "nasa.svs", Set.of("svs"));
    }

    @Override
    public String getName() {
        return "NASA (SVS)";
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        int count = 0;
        LocalDateTime start = startUpdateMedia();
        List<NasaSvsMedia> uploadedMedia = new ArrayList<>();
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            StringBuilder sb = new StringBuilder(API_SEARCH_ENDPOINT);
            ofNullable(getRuntimeData().getDoNotFetchEarlierThan())
                    .ifPresent(x -> sb.append("&release_date_gte=").append(x));
            String searchUrl = sb.toString();
            boolean done = false;
            while (!done) {
                try (CloseableHttpResponse response = httpclient.execute(newHttpGet(searchUrl));
                        InputStream in = response.getEntity().getContent()) {
                    NasaSvsSearchResultPage results = jackson.readValue(in, NasaSvsSearchResultPage.class);
                    LOGGER.info("GET {}", searchUrl);
                    for (NasaSvsSearchResult result : results.results()) {
                        if (!"Gallery".equals(result.result_type())) {
                            try {
                                LOGGER.debug("Updating {}", result.id());
                                updateImage(newId(result.id), uploadedMedia);
                            } catch (RuntimeException e) {
                                LOGGER.error("Error while processing {}", result, e);
                            }
                            count++;
                        }
                    }
                    done = results.next() == null;
                    if (!done) {
                        searchUrl = results.next().toExternalForm();
                    }
                }
            }
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private NasaSvsMedia updateImage(CompositeMediaId id, List<NasaSvsMedia> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        NasaSvsMedia media = null;
        Optional<NasaSvsMedia> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMedia(id);
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (shouldUploadAuto(media, false)) {
            media = saveMedia(upload(save ? saveMedia(media) : media, true, false).getLeft());
            uploadedMedia.add(media);
            save = false;
        }
        return save ? saveMedia(media) : media;
    }

    private NasaSvsMedia fetchMedia(CompositeMediaId id) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(API_VIZUAL_ENDPOINT + id.getMediaId()));
                InputStream in = response.getEntity().getContent()) {
            return mapMedia(jackson.readValue(in, NasaSvsVizualisation.class));
        }
    }

    private static CompositeMediaId newId(int id) {
        return new CompositeMediaId("svs", Integer.toString(id));
    }

    private NasaSvsMedia mapMedia(NasaSvsVizualisation viz) {
        NasaSvsMedia media = new NasaSvsMedia();
        media.setId(newId(viz.id()));
        media.setTitle(viz.title());
        media.setStudio(viz.studio());
        media.setPublicationDateTime(viz.release_date());
        ofNullable(viz.credits()).map(x -> x.stream().map(NasaSvsCredits::person).distinct().collect(joining(", ")))
                .ifPresent(media::setCredits);
        ofNullable(viz.keywords()).map(TreeSet::new).ifPresent(media::setKeywords);
        ofNullable(viz.missions()).map(TreeSet::new).ifPresent(media::setMissions);
        if (viz.media_groups() != null) {
            for (NasaSvsMediaGroup group : viz.media_groups()) {
                // For each group only consider the biggest media (unless we have a webm file at
                // the same resolution)
                group.mediaItemsStream()
                        .filter(x -> x.media_type().shouldBeOkForCommons()
                                && !x.url().toExternalForm().endsWith(".exr")
                                && !x.alt_text().startsWith("Time slates for the multiple movies")
                                && !x.alt_text().startsWith("This timeline is synchronized with"))
                        .sorted(comparingLong(NasaSvsMediaItem::pixels).reversed()).findFirst().ifPresent(biggest -> {
                            NasaSvsMediaItem item = group.mediaItemsStream()
                                    .filter(x -> x.url().toExternalForm().endsWith(".webm")
                                            && ((x.width() >= biggest.width() && x.height() >= biggest.height())
                                                    || (x.width() == 0 && x.height() == 0)))
                                    .sorted(comparingLong(NasaSvsMediaItem::pixels).reversed()).findFirst()
                                    .orElse(biggest);
                            addMetadata(media, item.url(), fm -> {
                                fm.setDescription(item.alt_text());
                                fm.setImageDimensions(new ImageDimensions(item.width(), item.height()));
                            });
                        });
            }
            viz.media_groups().stream().flatMap(x -> x.mediaItemsStream())
                    .filter(x -> x.media_type() == NasaSvsMediaType.Image)
                    .sorted(comparingLong(NasaSvsMediaItem::pixels)).map(NasaSvsMediaItem::url).findFirst()
                    .ifPresent(media::setThumbnailUrl);
        }
        return media;
    }

    @Override
    public URL getSourceUrl(NasaSvsMedia media, FileMetadata metadata) {
        return newURL("https://svs.gsfc.nasa.gov/" + media.getIdUsedInOrg());
    }

    @Override
    protected NasaSvsMedia refresh(NasaSvsMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId()));
    }

    @Override
    protected String getAuthor(NasaSvsMedia media) {
        return "NASA's Scientific Visualization Studio - " + media.getCredits();
    }

    @Override
    protected Class<NasaSvsMedia> getMediaClass() {
        return NasaSvsMedia.class;
    }

    // https://svs.gsfc.nasa.gov/help/

    @Override
    protected Optional<String> getOtherFields(NasaSvsMedia media) {
        StringBuilder sb = new StringBuilder();
        addOtherField(sb, "Keyword", media.getKeywords());
        String s = sb.toString();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    @Override
    public Set<String> findCategories(NasaSvsMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(media.getKeywordStream().map(mappings.getNasaKeywords()::get).filter(Objects::nonNull).toList());
        media.getMissions().stream()
                .forEach(x -> findCategoryFromMapping(x, "mission", mappings.getNasaMissions()).ifPresent(result::add));
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaSvsMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    public static record NasaSvsSearchResultPage(
            /**
             * The total number of results for the given query, regardless of which page or
             * how many results are currently being shown.
             */
            int count,
            /**
             * The URL that will display the next page of results, using the same limit as
             * the current request. This field will be null if all of the results are being
             * shown, or if the final page of results is being shown.
             */
            URL next,
            /**
             * The URL that will display the previous page of results, using the same limit
             * as the current request. This field will be null if all of the results are
             * being shown, or if the first page of results is being shown.
             */
            URL previous, /** An array of results objects. */
            List<NasaSvsSearchResult> results) {
    }

    public static record NasaSvsSearchResult(
            /** The type of result that this is. */
            String result_type,
            /** The internal database ID of the visualization. */
            int id,
            /** The url that the visualization can be accessed from. */
            URL url,
            /** The date and time (ET) the visualization was released. */
            ZonedDateTime release_date,
            /**
             * The total number of views that this particular visualization has gotten over
             * the past 10 days. This is used for popularity ranking on the site.
             */
            int hits,
            /** The title of the visualization. */
            String title,
            /**
             * A description of the visualization. The length of this will vary greatly
             * based on the page content.
             */
            String description) {
    }
}
