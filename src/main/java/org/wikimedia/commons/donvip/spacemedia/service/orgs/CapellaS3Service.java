package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.CapellaS3Service.StacMetadata.StacProperties;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Service
public class CapellaS3Service extends AbstractOrgS3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapellaS3Service.class);

    @Autowired
    private ObjectMapper jackson;

    @Value("${capella.stac.catalog}")
    private String stacCatalog;

    @Value("${capella.stac.endpoint}")
    private String stacEndpoint;

    @Autowired
    public CapellaS3Service(S3MediaRepository repository,
            @Value("${capella.s3.region}") Regions region,
            @Value("${capella.s3.buckets}") Set<String> buckets) {
        super(repository, "capella", region, buckets);
    }

    @Override
    public String getName() {
        return "Capella Space";
    }

    @Override
    protected S3Media enrichS3Media(S3Media media) throws IOException {
        CompositeMediaId id = media.getId();
        String[] items = id.getMediaId().split("/");
        if (items.length == 6) {
            int year = Integer.parseInt(items[1]);
            int month = Integer.parseInt(items[2]);
            int day = Integer.parseInt(items[3]);
            String img = items[4];
            media.setTitle(img);
            LOGGER.info("Enriching {}...", media);
            StacProperties properties = jackson.readValue(newURL(String.format(
                    "%s/%s-by-datetime/%s-%d/%s-%d-%d/%s-%d-%d-%d/%s/%s.json", stacEndpoint, stacCatalog, stacCatalog,
                    year, stacCatalog, year, month, stacCatalog, year, month, day, img, img)), StacMetadata.class)
                    .getProperties();
            media.setLongitude(properties.getProjCentroid().get(0));
            media.setLatitude(properties.getProjCentroid().get(1));
            media.setCreationDateTime(properties.getDatetime());
        } else {
            LOGGER.error("Unrecognized object key: {}", id);
        }
        media.setThumbnailUrl(newURL(getUrl(id).replace(".tif", "_thumb.png")));
        return media;
    }

    @Override
    public Set<String> findCategories(S3Media media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.add("Images by Capella");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(S3Media media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("Cc-by-4.0");
        return result;
    }

    @Override
    protected String getAuthor(S3Media media) throws MalformedURLException {
        // https://registry.opendata.aws/capella_opendata/
        return "Capella Space";
    }

    @Override
    protected Set<String> getEmojis(S3Media uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.EARTH_AMERICA);
        result.add(Emojis.SATELLITE);
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(S3Media uploadedMedia) {
        return Set.of("@capellaspace");
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StacMetadata {
        private String id;
        private StacProperties properties;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public StacProperties getProperties() {
            return properties;
        }

        public void setProperties(StacProperties properties) {
            this.properties = properties;
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StacProperties {
            private ZonedDateTime datetime;
            private String platform;
            @JsonProperty("proj:centroid")
            private List<Double> projCentroid;

            public ZonedDateTime getDatetime() {
                return datetime;
            }

            public void setDatetime(ZonedDateTime datetime) {
                this.datetime = datetime;
            }

            public String getPlatform() {
                return platform;
            }

            public void setPlatform(String platform) {
                this.platform = platform;
            }

            public List<Double> getProjCentroid() {
                return projCentroid;
            }

            public void setProjCentroid(List<Double> projCentroid) {
                this.projCentroid = projCentroid;
            }
        }
    }
}
