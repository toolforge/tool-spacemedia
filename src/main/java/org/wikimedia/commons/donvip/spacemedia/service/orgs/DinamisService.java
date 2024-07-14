package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.uriExists;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.DinamisMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.DinamisMedia.Mode;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.DinamisMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.api.DinamisFeature;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.api.DinamisFeatureCollection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis.api.DinamisProperties;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

public class DinamisService extends AbstractOrgService<DinamisMedia> {

    private static final String BASE_URL = "https://openspot-dinamis.data-terra.org";
    private static final String API_URL = "https://openspot-dinamis.data-terra.org/api";

    @Autowired
    private DinamisService self;

    public DinamisService(DinamisMediaRepository repository) {
        super(repository, "dinamis", Set.of("FR", "PO"));
    }

    @Override
    public String getName() {
        return "Dinamis";
    }

    @Override
    protected Class<DinamisMedia> getMediaClass() {
        return DinamisMedia.class;
    }

    @Override
    public Set<String> findLicenceTemplates(DinamisMedia media, FileMetadata metadata) {
        return Set.of("Licence Ouverte 2");
    }

    private String getBaseUrl(String repoId) {
        return BASE_URL + ("PO".equals(repoId) ? "/PO" : "");
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<DinamisMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (String repoId : getRepoIdsFromArgs(args)) {
            Document html = getWithJsoup(getBaseUrl(repoId), 10_000, 3);
            List<String> modes = html.getElementById("choix_resolution").getElementsByTag("option").stream()
                    .map(x -> x.attr("value")).toList();
            for (Element option : html.getElementById("choix_annee").getElementsByTag("option")) {
                String value = option.attr("value");
                if (isBlank(value)) {
                    value = option.text();
                }
                for (String mode : modes) {
                    String collection = value + "_" + mode;
                    for (DinamisFeature feature : self.getCollection(collection).features()) {
                        updateImage(getMediaId(feature.properties(), collection), uploadedMedia);
                        ongoingUpdateMedia(start, count++);
                    }
                }
            }
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    @Cacheable("dinamisCollections")
    public DinamisFeatureCollection getCollection(String collection) throws IOException {
        return jackson.readValue(newURL(API_URL + "/geo/" + collection), DinamisFeatureCollection.class);
    }

    private DinamisMedia updateImage(CompositeMediaId id, List<DinamisMedia> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        DinamisMedia media = null;
        Optional<DinamisMedia> imageInDb = repository.findById(id);
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

    private DinamisMedia fetchMedia(CompositeMediaId id) throws IOException {
        String[] tab = id.getMediaId().split(":");
        return mapMedia(self.getCollection(tab[0]).features().stream()
                .filter(x -> id.getRepoId().equals(x.properties().page_())
                        && tab[1].equals(Integer.toString(x.properties().fid())))
                .findFirst().orElseThrow(() -> new IllegalStateException("No feature found for id " + id)), tab[0]);
    }

    private static CompositeMediaId getMediaId(DinamisProperties props, String collection) {
        return new CompositeMediaId(props.page_(), collection + ":" + props.fid());
    }

    DinamisMedia mapMedia(DinamisFeature feature, String collection) {
        DinamisMedia media = new DinamisMedia();
        DinamisProperties props = feature.properties();
        media.setId(getMediaId(props, collection));
        media.setTitle(props.ds_name());
        media.setThumbnailUrl(newURL(API_URL + "/lien_jpg/" + props.f_quicklook()));
        media.setCreationDate(props.img_date());
        media.setPublicationYear(Year.of(props.img_date().getYear()));
        media.setMode(Mode.valueOf(props.mode_()));
        media.setResolution(props.resolution());
        media.setPolygon(new Polygon(
                feature.geometry().coordinates().stream().map(x -> new Point(x.get(0).get(0), x.get(0).get(1)))
                        .toList()));
        String apiUrlTif = API_URL + "/lien_tif/";
        String url = apiUrlTif + props.ds_name() + ".TIF";
        if (!uriExists(url)) {
            url = apiUrlTif + props.ds_name().replace("ORT_", "IMG_").replace("---", "_") + "_R1C1.TIF";
        }
        if (!uriExists(url)) {
            url = apiUrlTif + props.ds_name() + ".jp2";
        }
        if (uriExists(url)) {
            addMetadata(media, url, null);
        } else {
            // FIXME HTTP 403
            throw new IllegalStateException("Failed to find file URI for " + feature);
        }
        return media;
    }

    @Override
    public URL getSourceUrl(DinamisMedia media, FileMetadata metadata, String ext) {
        return newURL(getBaseUrl(media.getId().getRepoId()));
    }

    @Override
    protected Optional<String> getOtherFields(DinamisMedia media) {
        StringBuilder sb = new StringBuilder();
        List<Point> bb = media.getPolygon().getPoints();
        addOtherField(sb, "Bounding box",
                "{{Map/bbox|longitude=" + bb.get(0) + "/" + bb.get(1) + "|latitude=" + bb.get(2) + "/" + bb.get(3)
                        + "}}");
        return Optional.of(sb.toString());
    }

    @Override
    protected DinamisMedia refresh(DinamisMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId()));
    }
}
