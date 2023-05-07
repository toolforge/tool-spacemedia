package org.wikimedia.commons.donvip.spacemedia.data.domain;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithLatLon {

    public double getLatitude();

    public void setLatitude(double latitude);

    public double getLongitude();

    public void setLongitude(double longitude);

    @Transient
    @JsonIgnore
    public double getPrecision();
}
