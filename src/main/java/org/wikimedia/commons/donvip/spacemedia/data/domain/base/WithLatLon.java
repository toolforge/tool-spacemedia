package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import jakarta.persistence.Transient;

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
