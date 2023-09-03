package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.NasaSvsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.NasaSvsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsCredits;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsMediaItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api.NasaSvsVizualisation;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NasaSvsService extends AbstractOrgService<NasaSvsMedia> {

    private static final String API_SEARCH_ENDPOINT = "https://svs.gsfc.nasa.gov/api/search/?sort_by_0=DESC";
    private static final String API_VIZUAL_ENDPOINT = "https://svs.gsfc.nasa.gov/api/";

    @Autowired
    private ObjectMapper jackson;

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
    public void updateMedia() throws IOException, UploadException {
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
                    for (NasaSvsSearchResult result : results.results()) {
                        updateImage(newId(result.pk), uploadedMedia);
                        count++;
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
        media.setCredits(viz.credits().stream().map(NasaSvsCredits::person).collect(joining(", ")));
        media.setKeywords(new TreeSet<>(viz.keywords()));
        NasaSvsMediaItem main = viz.main_video() != null ? viz.main_video() : viz.main_image();
        media.setDescription(main.alt_text());
        media.setType(main.media_type());
        addMetadata(media, main.url(), fm -> fm.setImageDimensions(new ImageDimensions(main.width(), main.height())));
        viz.media_groups().stream().flatMap(x -> x.media().stream())
                .filter(x -> x.media_type() == NasaSvsMediaType.Image)
                .sorted(Comparator.comparingLong(NasaSvsMediaItem::pixels)).map(NasaSvsMediaItem::url)
                .findFirst().ifPresent(media::setThumbnailUrl);
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
    protected String getAuthor(NasaSvsMedia media) throws MalformedURLException {
        return "NASA's Scientific Visualization Studio - " + media.getCredits();
    }

    @Override
    protected Class<NasaSvsMedia> getMediaClass() {
        return NasaSvsMedia.class;
    }

    // https://svs.gsfc.nasa.gov/help/

    public static record NasaSvsSearchResultPage( /**
                                                   * The total number of results for the given query, regardless of
                                                   * which page or how many results are currently being shown.
                                                   */
    int count, /**
                * The URL that will display the next page of results, using the same limit as
                * the current request. This field will be null if all of the results are being
                * shown, or if the final page of results is being shown.
                */
    URL next, /**
               * The URL that will display the previous page of results, using the same limit
               * as the current request. This field will be null if all of the results are
               * being shown, or if the first page of results is being shown.
               */
    URL previous, /** An array of results objects. */
    List<NasaSvsSearchResult> results) {
    }

    public static record NasaSvsSearchResult( /** The type of result that this is. */
    String result_type, /** The internal database ID of the visualization. */
    int pk, /** The url that the visualization can be accessed from. */
    URL url, /** The date and time (ET) the visualization was released. */
    ZonedDateTime release_date, /**
                                 * The total number of views that this particular visualization has gotten over
                                 * the past 10 days. This is used for popularity ranking on the site.
                                 */
    int hits, /** The title of the visualization. */
    String title, /**
                   * A description of the visualization. The length of this will vary greatly
                   * based on the page content.
                   */
    String description) {
    }
}
