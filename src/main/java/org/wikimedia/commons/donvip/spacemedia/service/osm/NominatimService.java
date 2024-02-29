package org.wikimedia.commons.donvip.spacemedia.service.osm;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Lazy
@Service
public class NominatimService {

    @Value("${nominatim.url}")
    private URL nominatimUrl;

    @Autowired
    private ObjectMapper jackson;

    public ReverseResponse reverse(double lat, double lon, int zoom) throws IOException {
        return jackson.readValue(
                newURL(String.format(
                        "%s/reverse?lat=%f&lon=%f&format=jsonv2&addressdetails=1&accept-language=en-US&zoom=%d",
                        nominatimUrl.toExternalForm(), lat, lon, zoom)),
                ReverseResponse.class);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReverseResponse(long place_id, String licence, String osm_type, long osm_id, double lat, double lon,
            int place_rank, String category, String type, double importance, String addresstype, String display_name,
            String name, Address address) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(String road, String village, String state_district, String state, String postcode,
            String country, String country_code) {
    }
}
