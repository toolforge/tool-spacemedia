package org.wikimedia.commons.donvip.spacemedia.data.domain.webmil;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

@Entity(name = "WebmilMedia")
public class WebMilMedia extends SingleFileMedia {

    /**
     * VIRIN of asset.
     */
    @Column(length = 20)
    private String virin;

    public String getVirin() {
        return virin;
    }

    public void setVirin(String virin) {
        this.virin = virin;
    }

    public WebMilMedia copyDataFrom(WebMilMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        setVirin(mediaFromApi.getVirin());
        return this;
    }

    @Override
    public String toString() {
        return "WebMilMedia [virin=" + virin + ", title=" + getTitle() + ", credits=" + getCredits()
                + ", publicationDate=" + getPublicationDate() + ", id=" + getId() + ']';
    }
}
