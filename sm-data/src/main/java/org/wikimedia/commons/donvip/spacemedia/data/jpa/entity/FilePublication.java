package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public class FilePublication extends Publication {

    private ZonedDateTime captureDateTime;

    @ManyToMany(mappedBy = "filePublications", cascade = CascadeType.REMOVE)
    private Set<WebPublication> webPublications;

    @ManyToMany(mappedBy = "filePublications", cascade = CascadeType.REMOVE)
    private Set<MediaPublication> mediaPublications;

    @ManyToOne
    private File file;

    public FilePublication() {

    }

    public FilePublication(Depot depot, String id, URL url) {
        setId(new PublicationKey(depot.getId(), id));
        setDepot(depot);
        setUrl(url);
    }

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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(captureDateTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        FilePublication other = (FilePublication) obj;
        return Objects.equals(captureDateTime, other.captureDateTime);
    }
}
