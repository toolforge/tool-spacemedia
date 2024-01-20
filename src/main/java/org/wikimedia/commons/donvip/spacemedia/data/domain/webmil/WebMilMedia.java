package org.wikimedia.commons.donvip.spacemedia.data.domain.webmil;

import java.util.List;
import java.util.Optional;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

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

    @Override
    protected String getUploadId(FileMetadata fileMetadata) {
        return Optional.ofNullable(getVirin()).orElseGet(() -> super.getUploadId(fileMetadata));
    }

    @Override
    public List<String> getIdUsedInCommons() {
        return List.of(getIdUsedInOrg(), getVirin());
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
