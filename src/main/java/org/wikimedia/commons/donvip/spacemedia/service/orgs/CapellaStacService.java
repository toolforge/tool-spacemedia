package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.osm.NominatimService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class CapellaStacService extends AbstractOrgStacService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapellaStacService.class);

    private static final Map<String, String> SATS = Map.ofEntries(e("01", "Q124124084"), e("02", "Q124124108"),
            e("03", "Q124124120"), e("04", "Q124124133"), e("05", "Q124124202"), e("06", "Q124124533"),
            e("07", "Q124126674"), e("08", "Q124126687"), e("09", "Q124126690"), e("10", "Q124126999"),
            e("11", "Q124127134"));

    private static final Pattern SAT_PATTERN = Pattern.compile("CAPELLA_C(\\d{2})_.*");

    @Autowired
    private NominatimService nominatim;

    @Autowired
    public CapellaStacService(StacMediaRepository repository,
            @Value("${capella.stac.catalogs}") Set<String> catalogs) {
        super(repository, "capella", catalogs);
    }

    @Override
    public String getName() {
        return "Capella";
    }

    @Override
    protected void enrichStacMedia(StacMedia media) {
        try {
            media.setTitle(nominatim.reverse(media.getLatitude(), media.getLongitude(), 10).display_name());
            if (isBlank(media.getTitle())) {
                LOGGER.warn("Nominatim empty response for /reverse lat={}, lon={}", media.getLatitude(),
                        media.getLongitude());
                media.setTitle(media.getIdUsedInOrg());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected boolean isStacItemBefore(String itemHref, LocalDate doNotFetchEarlierThan) {
        String s = itemHref.substring(itemHref.lastIndexOf('_') + 1);
        return LocalDate.of(
                parseInt(s.substring(0, 4)),
                parseInt(s.substring(4, 6)),
                parseInt(s.substring(6, 8))).isBefore(doNotFetchEarlierThan);
    }

    @Override
    protected boolean isStacItemIgnored(String itemHref) {
        return itemHref.contains("_SICD_") || itemHref.contains("_SIDD_") || itemHref.contains("_CPHD_");
    }

    @Override
    protected boolean categorizeGeolocalizedByName() {
        return true;
    }

    @Override
    public Set<String> findLicenceTemplates(StacMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("Cc-by-4.0");
        return result;
    }

    @Override
    protected String getAuthor(StacMedia media, FileMetadata metadata) {
        // https://registry.opendata.aws/capella_opendata/
        return "Capella Space";
    }

    @Override
    protected SdcStatements getStatements(StacMedia media, FileMetadata metadata) {
        return super.getStatements(media, metadata)
                .creator(media.getIdUsedInOrg(), SAT_PATTERN, SATS) // Created by Capella-XX
                .locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod("Q725252") // Satellite imagery
                .capturedWith("Q740686"); // Taken with SAR
    }

    @Override
    protected Set<String> getEmojis(StacMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.EARTH_AMERICA);
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(StacMedia uploadedMedia) {
        return Set.of("@capellaspace");
    }
}
