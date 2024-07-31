package org.wikimedia.commons.donvip.spacemedia.service.dvids;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiAssetResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiPageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiSearchResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiUnitResponse;
import org.wikimedia.commons.donvip.spacemedia.exception.ApiException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;

/**
 * Service fetching images from https://api.dvidshub.net/
 */
@Lazy
@Service
public class DvidsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DvidsService.class);

    private static final int MAX_RESULTS = 1000;

    @Value("${dvids.api.key}")
    private String apiKey;

    @Value("${dvids.api.search.url}")
    private String searchApiEndpoint;

    @Value("${dvids.api.search.year.url}")
    private String searchYearApiEndpoint;

    @Value("${dvids.api.asset.url}")
    private UriTemplate assetApiEndpoint;

    @Value("${dvids.api.unit.url}")
    private UriTemplate unitApiEndpoint;

    private final RestTemplate rest = new RestTemplate();

    public DvidsMedia getMediaFromApi(CompositeMediaId id) {
        DvidsMedia media = ofNullable(
                rest.getForObject(assetApiEndpoint.expand(Map.of("api_key", apiKey, "id", id.getMediaId())),
                        ApiAssetResponse.class))
                .orElseThrow(() -> new IllegalArgumentException("No result from DVIDS API for " + id)).results();
        media.setId(id);
        return media;
    }

    public ApiSearchResponse searchDvidsMediaIds(boolean allowCappedResults, DvidsMediaType type,
            String unit, String country, int year, int month, int page) throws ApiException, TooManyResultsException {
        Map<String, Object> variables = new TreeMap<>(Map.of("api_key", apiKey, "type", type, "page", page));
        String template = year <= 0 ? searchApiEndpoint : searchYearApiEndpoint;
        if ("*".equals(unit)) {
            template = template.replace("&unit={unit}", "");
        } else {
            variables.put("unit", unit);
        }
        if ("*".equals(country)) {
            template = template.replace("&country={country}", "");
        } else {
            variables.put("country", country);
        }
        if (year > 0) {
            variables.put("from_date", String.format("%04d-%02d-01T00:00:00Z", year, month));
            variables.put("to_date", String.format("%04d-%02d-%02dT23:59:59Z", year, month == 12 ? 12 : month + 1,
                    month == 12 ? 31 : 1));
        }
        ApiSearchResponse response = rest.getForObject(new UriTemplate(template).expand(variables),
                ApiSearchResponse.class);
        if (response == null || response.getErrors() != null) {
            throw new ApiException(
                    String.format("API error while fetching DVIDS %ss from unit '%s': %s", type, unit, response));
        }
        ApiPageInfo pageInfo = response.getPageInfo();
        if (pageInfo.totalResults() == MAX_RESULTS) {
            String msg = String.format(
                    "Incomplete search! More criteria must be defined for %ss of '%s'/'%s' (%04d-%02d)!", type, unit,
                    country, year, month);
            if (allowCappedResults) {
                LOGGER.warn(msg);
            } else {
                throw new TooManyResultsException(msg);
            }
        } else if (pageInfo.totalResults() == 0) {
            LOGGER.warn("No {} for {}/{} in year {}-{}", type, unit, country, year, month);
        } else if (page == 1) {
            LOGGER.debug("{} {}s to process for {}/{}", pageInfo.totalResults(), type, unit, country);
        }
        return response;
    }

    @Cacheable("unitAbbrByFullName")
    public String getUnitAbbreviation(String unitFullName) {
        return ofNullable(rest.getForObject(
                unitApiEndpoint.expand(Map.of("api_key", apiKey, "unit_name", requireNonNull(unitFullName))),
                ApiUnitResponse.class)).orElseThrow().results().iterator().next().unit_abbrev();
    }
}
