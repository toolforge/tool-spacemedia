package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
public class FilePublication extends Publication {

    private ZonedDateTime captureDateTime;

    @ManyToMany(mappedBy = "filePublications")
    private Set<WebPublication> webPublications;

    @ManyToMany(mappedBy = "filePublications")
    private Set<MediaPublication> mediaPublications;

    public ZonedDateTime getCaptureDateTime() {
        return captureDateTime;
    }

    public void setCaptureDateTime(ZonedDateTime captureDateTime) {
        this.captureDateTime = captureDateTime;
    }

    public Set<WebPublication> getWebPublications() {
        return webPublications;
    }

    public void setWebPublications(Set<WebPublication> webPublications) {
        this.webPublications = webPublications;
    }

    public Set<MediaPublication> getMediaPublications() {
        return mediaPublications;
    }

    public void setMediaPublications(Set<MediaPublication> mediaPublications) {
        this.mediaPublications = mediaPublications;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(captureDateTime, webPublications, mediaPublications);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FilePublication other = (FilePublication) obj;
        return Objects.equals(captureDateTime, other.captureDateTime)
                && Objects.equals(webPublications, other.webPublications)
                && Objects.equals(mediaPublications, other.mediaPublications);
    }
}
