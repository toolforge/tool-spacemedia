package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;

@Embeddable
public class DvidsLocation {

    private String city;

    private String state;

    private String country;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return Arrays.asList(city, state, country).stream().filter(StringUtils::isNotBlank).collect(Collectors.joining(", "));
    }
}
