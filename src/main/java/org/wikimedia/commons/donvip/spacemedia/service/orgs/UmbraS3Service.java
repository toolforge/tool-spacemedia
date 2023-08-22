package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.UmbraS3Service.UmbraMetadata.Collect;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Service
public class UmbraS3Service extends AbstractOrgS3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmbraS3Service.class);

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    public UmbraS3Service(S3MediaRepository repository,
            @Value("${umbra.s3.region}") Regions region,
            @Value("${umbra.s3.buckets}") Set<String> buckets) {
        // http://umbra-open-data-catalog.s3-website.us-west-2.amazonaws.com
        super(repository, "umbra", region, buckets);
    }

    @Override
    public String getName() {
        return "Umbra";
    }

    @Override
    protected S3Media enrichS3Media(S3Media media) throws IOException {
        S3MediaId id = media.getId();
        String[] items = id.getObjectKey().split("/");
        if (items.length == 6) {
            media.setTitle(items[2] + " (" + items[4] + ')');
        } else {
            LOGGER.error("Unrecognized object key: {}", id);
        }
        LOGGER.info("Enriching {}...", media);
        try (InputStream in = getS3Object(id.getBucketName(),
                id.getObjectKey().replace("GEC.tif", "METADATA.json").replace(".tif", "_METADATA.json"))
                .getObjectContent()) {
            Collect collect = jackson.readValue(in, UmbraMetadata.class).getCollects().get(0);
            List<Double> center = collect.getSceneCenterPointLla().getCoordinates();
            media.setCreationDateTime(collect.getStartAtUTC()
                    .plus(Duration.between(collect.getStartAtUTC(), collect.getEndAtUTC()).dividedBy(2)));
            media.setLongitude(center.get(0));
            media.setLatitude(center.get(1));
        }
        return media;
    }

    @Override
    public Set<String> findCategories(S3Media media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.add("Images by Umbra");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(S3Media media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("Cc-by-sa-4.0");
        return result;
    }

    @Override
    protected String getAuthor(S3Media media) throws MalformedURLException {
        // https://umbra.space/terms-of-use
        return "Umbra Lab, Inc.";
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
        return Set.of("@umbraspace");
    }


    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UmbraMetadata {
        private String vendor;
        private String umbraSatelliteName;
        private List<Collect> collects;

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public String getUmbraSatelliteName() {
            return umbraSatelliteName;
        }

        public void setUmbraSatelliteName(String umbraSatelliteName) {
            this.umbraSatelliteName = umbraSatelliteName;
        }

        public List<Collect> getCollects() {
            return collects;
        }

        public void setCollects(List<Collect> collects) {
            this.collects = collects;
        }

        @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Collect {
            private ZonedDateTime startAtUTC;
            private ZonedDateTime endAtUTC;
            private String radarBand;
            private Point sceneCenterPointLla;

            public ZonedDateTime getStartAtUTC() {
                return startAtUTC;
            }

            public void setStartAtUTC(ZonedDateTime startAtUTC) {
                this.startAtUTC = startAtUTC;
            }

            public ZonedDateTime getEndAtUTC() {
                return endAtUTC;
            }

            public void setEndAtUTC(ZonedDateTime endAtUTC) {
                this.endAtUTC = endAtUTC;
            }

            public String getRadarBand() {
                return radarBand;
            }

            public void setRadarBand(String radarBand) {
                this.radarBand = radarBand;
            }

            public Point getSceneCenterPointLla() {
                return sceneCenterPointLla;
            }

            public void setSceneCenterPointLla(Point sceneCenterPointLla) {
                this.sceneCenterPointLla = sceneCenterPointLla;
            }
        }

        @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Point {
            private List<Double> coordinates;

            public List<Double> getCoordinates() {
                return coordinates;
            }

            public void setCoordinates(List<Double> coordinates) {
                this.coordinates = coordinates;
            }
        }
    }
}
