package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.GeometryService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.UmbraS3Service.UmbraMetadata.Collect;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Service
public class UmbraS3Service extends AbstractOrgS3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmbraS3Service.class);

    private static final Map<String, String> SATS = Map.of("01", "Q121841901", "02", "Q121841939", "03", "Q121841944",
            "04", "Q121841948", "05", "Q121841953", "06", "Q121841955", "07", "Q121842030", "08", "Q121842041");

    private static final Pattern SAT_PATTERN = Pattern.compile(".*_UMBRA-(\\d{2})/.*");

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    private GeometryService geometry;

    @Autowired
    public UmbraS3Service(S3MediaRepository repository,
            @Value("${umbra.s3.region}") Regions region,
            @Value("${umbra.s3.buckets}") Set<String> buckets) {
        super(repository, "umbra", region, buckets);
    }

    @Override
    public String getName() {
        return "Umbra";
    }

    @Override
    protected S3Media enrichS3Media(S3Media media) throws IOException {
        CompositeMediaId id = media.getId();
        String[] items = id.getMediaId().split("/");
        if (items.length == 5) {
            media.setTitle(items[3]);
        }else if (items.length == 6) {
            media.setTitle(items[2] + " (" + items[4] + ')');
        } else if (items.length == 7) {
            media.setTitle(items[3] + " (" + items[5] + ')');
        } else if (items.length == 8) {
            media.setTitle(items[4] + " (" + items[6] + ", " + items[3].split(" ")[0] + ')');
        } else if (items.length == 9) {
            media.setTitle(items[7] + " (" + items[8].replace(".tif", "") + ')');
        } else {
            LOGGER.error("Unrecognized object key: {}", id);
        }
        LOGGER.info("Enriching {}...", media);
        try (InputStream in = getS3Object(id.getRepoId(),
                id.getMediaId().replace("GEC.tif", "METADATA.json").replace(".tif", "_METADATA.json"))
                .getObjectContent()) {
            List<Collect> collects = jackson.readValue(in, UmbraMetadata.class).getCollects();
            if (isNotEmpty(collects)) {
                Collect collect = collects.get(0);
                List<Double> center = collect.getSceneCenterPointLla().getCoordinates();
                media.setCreationDateTime(collect.getStartAtUTC()
                        .plus(Duration.between(collect.getStartAtUTC(), collect.getEndAtUTC()).dividedBy(2)));
                media.setLongitude(center.get(0));
                media.setLatitude(center.get(1));
            } else {
                LOGGER.warn("No collect for {}", id);
            }
        }
        return media;
    }

    @Override
    public Set<String> findCategories(S3Media media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        String continent = geometry.getContinent(media.getLatitude(), media.getLongitude());
        result.add(continent != null ? "Images of " + continent + " by Umbra" : "Images by Umbra");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(S3Media media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("Cc-by-4.0");
        return result;
    }

    @Override
    protected String getAuthor(S3Media media) {
        // https://registry.opendata.aws/umbra-open-data/
        // https://umbra.space/terms-of-use
        return "Umbra Lab, Inc.";
    }

    @Override
    protected SdcStatements getStatements(S3Media media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        Matcher m = SAT_PATTERN.matcher(media.getId().getMediaId());
        if (m.matches()) {
            result.creator(SATS.get(m.group(1))); // Created by UMBRA-XX
        }
        return result.locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod("Q725252") // Satellite imagery
                .capturedWith("Q740686"); // Taken with SAR
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

    /**
     * Extended Umbra Metadata: https://docs.canopy.umbra.space/reference/get_schema
     */
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
