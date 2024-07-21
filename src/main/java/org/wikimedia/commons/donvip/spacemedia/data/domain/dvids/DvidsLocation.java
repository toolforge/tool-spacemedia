package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DvidsLocation {

    @Column(length = 96)
    private String city;

    @Column(length = 48)
    private String state;

    @Column(length = 48)
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
        return Arrays.asList(city, state, country).stream().filter(StringUtils::isNotBlank).collect(joining(", "));
    }
}
