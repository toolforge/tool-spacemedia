package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgStacService.getOtherFieldBoundingBox;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import org.wikimedia.commons.donvip.spacemedia.service.orgs.UmbraS3Service.UmbraMetadata.Collect;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Service
public class UmbraS3Service extends AbstractOrgS3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmbraS3Service.class);

    // Prefer GEC over CSI
    private static final String CSI_TIF = "_csi.tif";

    private static final Map<String, String> SATS = Map.of("01", "Q121841901", "02", "Q121841939", "03", "Q121841944",
            "04", "Q121841948", "05", "Q121841953", "06", "Q121841955", "07", "Q121842030", "08", "Q121842041",
            "09", "Q129218282", "10", "Q129218963");

    private static final Pattern SAT_PATTERN = Pattern.compile(".*_UMBRA-(\\d{2})/.*");

    private static final Pattern UUID_REGEX = Pattern
            .compile(".+/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/.+");

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
    protected String getSource(S3Media media, FileMetadata metadata) {
        StringBuilder sb = new StringBuilder("{{en|1=").append(wikiLink(metadata.getAssetUrl(), "Umbra Space"))
                .append(" via their AWS S3 Bucket");
        getStacItemUrl(media).ifPresent(itemUrl -> sb.append(". Further data via ")
                .append(wikiLink(itemUrl, "STAC API")).append(". View in ")
                .append(wikiLink(newURL(
                        itemUrl.toExternalForm().replace("://", "://radiantearth.github.io/stac-browser/#/external/")),
                        "STAC Browser")));
        return sb.append(".}}").toString();
    }

    @Override
    protected Optional<String> getOtherFields(S3Media media) {
        return getStacItemUrl(media).map(itemUrl -> getOtherFieldBoundingBox(jackson, itemUrl))
                .orElse(Optional.empty());
    }

    private static Optional<URL> getStacItemUrl(S3Media media) {
        Matcher m = UUID_REGEX.matcher(media.getUniqueMetadata().getAssetUrl().toExternalForm());
        if (m.matches()) {
            String uuid = m.group(1);
            return Optional.of(newURL("https://s3.us-west-2.amazonaws.com/umbra-open-data-catalog/stac/"
                    + media.getPublicationYear() + "/" + media.getPublicationMonth() + "/" + media.getPublicationDate()
                    + "/" + uuid + "/" + uuid + ".json"));
        }
        LOGGER.warn("No UUID found in image URL for {}", media);
        return Optional.empty();
    }

    @Override
    protected Optional<String> getPermission(S3Media media) {
        return Optional.of("{{en|See "
                + wikiLink(newURL("https://umbra.space/open-data/"), "Umbra-Open-Data Image Collection data license")
                + ". Reverse geocoding in description and category: "
                + wikiLink(newURL("https://www.openstreetmap.org/copyright"),
                        "Data © OpenStreetMap contributors, ODbL 1.0")
                + ". }}");
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Files from Umbra uploaded by " + commonsService.getAccount();
    }

    @Override
    protected boolean isSatellitePicture(S3Media media, FileMetadata metadata) {
        return true;
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected boolean skipMedia(S3Media media, List<S3Media> files) {
        String key = media.getIdUsedInOrg();
        return key.toLowerCase(Locale.ENGLISH).contains(CSI_TIF) && files.stream()
                .anyMatch(f -> f.getIdUsedInOrg()
                        .startsWith(key.substring(0, key.toLowerCase(Locale.ENGLISH).lastIndexOf(CSI_TIF)))
                        && !f.getIdUsedInOrg().toLowerCase(Locale.ENGLISH).contains(CSI_TIF));
    }

    @Override
    protected S3Media enrichS3Media(S3Media media) {
        CompositeMediaId id = media.getId();
        String[] items = id.getMediaId().split("/");
        if (items.length == 5) {
            media.setTitle(items[3]);
        } else if (items.length == 6) {
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
        List<S3ObjectSummary> jsonFiles = s3.getFiles(region, id.getRepoId(),
                id.getMediaId().substring(0, id.getMediaId().lastIndexOf('/')), Set.of("json"),
                Function.identity(), x -> x.getKey().endsWith("METADATA.json"),
                Comparator.comparing(S3ObjectSummary::getKey));
        if (jsonFiles.size() != 1) {
            throw new IllegalStateException("Did not find exactly one METADATA.json file: " + jsonFiles);
        }
        S3ObjectSummary jsonMetadata = jsonFiles.iterator().next();
        try (InputStream in = getS3Object(id.getRepoId(), jsonMetadata.getKey()).getObjectContent()) {
            List<Collect> collects = jackson.readValue(in, UmbraMetadata.class).collects();
            if (isNotEmpty(collects)) {
                Collect collect = collects.get(0);
                List<Double> center = collect.sceneCenterPointLla().coordinates();
                media.setCreationDateTime(collect.startAtUTC()
                        .plus(Duration.between(collect.startAtUTC(), collect.endAtUTC()).dividedBy(2)));
                media.setLongitude(center.get(0));
                media.setLatitude(center.get(1));
            } else {
                LOGGER.warn("No collect for {}", id);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return media;
    }

    @Override
    protected boolean categorizeGeolocalizedByName() {
        return true;
    }

    @Override
    public Set<String> findCategories(S3Media media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(findCategoriesFromTitleAndAffixes(media.getTitle(),
                new Affixes(List.of("Images of "), false),
                new Affixes(List.of(" by Umbra"), false)));
        if (replace(result, media.getYear() + " satellite pictures", media.getYear() + " Umbra images")) {
            result.remove("Images by Umbra");
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(S3Media media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("Cc-by-4.0");
        return result;
    }

    @Override
    protected String getAuthor(S3Media media, FileMetadata metadata) {
        // https://registry.opendata.aws/umbra-open-data/
        // https://umbra.space/terms-of-use
        return "Umbra Lab, Inc.";
    }

    @Override
    protected SdcStatements getStatements(S3Media media, FileMetadata metadata) {
        return super.getStatements(media, metadata)
                .creator(media.getIdUsedInOrg(), SAT_PATTERN, SATS) // Created by UMBRA-XX
                .locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod(Q725252_SATELLITE_IMAGERY)
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
    public static record UmbraMetadata (
        String vendor,
        String umbraSatelliteName,
        List<Collect> collects) {

        @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record Collect(
            ZonedDateTime startAtUTC,
            ZonedDateTime endAtUTC,
            String radarBand,
            Point sceneCenterPointLla) {
        }

        @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record Point(List<Double> coordinates) {
        }
    }
}
