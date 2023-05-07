package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.time.ZonedDateTime;
import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class NasaSdoAiaKeywords {

    /**
     * T_OBS time: shutter open start time plus the middle of the exposure time
     */
    private ZonedDateTime tObs;

    /**
     * FSN: least significant 30b of AHTLFSN; Frame Serial Number (int)
     */
    private Integer fsn;

    /**
     * WAVELNTH: wavelength of this observation, with 2 each for camera (telescope)
     * 1, 2, 4, and 4 each for camera 3 (as a float in nm (Phil)), and with mapping
     * reference number of each wavelength in ( ):
     */
    private Short wavelength;

    /**
     * CAMERA: most significant 2b of AHTLFSN +1 = [1, 2, 3, 4]; AIA camera
     * (telescope) number associated with the image (int)
     */
    private Short camera;

    /**
     * EXPTIME: the shutter open time duration. Floating point, calculated in double
     * precision, exposure time in seconds
     */
    private Double expTime;

    public ZonedDateTime gettObs() {
        return tObs;
    }

    public void settObs(ZonedDateTime tObs) {
        this.tObs = tObs;
    }

    public Integer getFsn() {
        return fsn;
    }

    public void setFsn(Integer fsn) {
        this.fsn = fsn;
    }

    public Short getWavelength() {
        return wavelength;
    }

    public void setWavelength(Short wavelength) {
        this.wavelength = wavelength;
    }

    public Short getCamera() {
        return camera;
    }

    public void setCamera(Short camera) {
        this.camera = camera;
    }

    public Double getExpTime() {
        return expTime;
    }

    public void setExpTime(Double expTime) {
        this.expTime = expTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera, expTime, fsn, tObs, wavelength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        NasaSdoAiaKeywords other = (NasaSdoAiaKeywords) obj;
        return Objects.equals(camera, other.camera) && Objects.equals(expTime, other.expTime)
                && Objects.equals(fsn, other.fsn) && Objects.equals(tObs, other.tObs)
                && Objects.equals(wavelength, other.wavelength);
    }

    @Override
    public String toString() {
        return "NasaSdoAiaKeywords [tObs=" + tObs + ", fsn=" + fsn + ", wavelength=" + wavelength + ", camera=" + camera
                + ", expTime=" + expTime + "]";
    }
}
