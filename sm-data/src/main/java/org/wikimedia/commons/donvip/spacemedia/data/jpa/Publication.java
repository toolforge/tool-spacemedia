package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EmbeddedId;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;

@MappedSuperclass
public abstract class Publication {

    @EmbeddedId
    private PublicationKey id;

    @MapsId("depotID")
    @ManyToOne
    private Depot depot;

    private ZonedDateTime publicationDateTime;

    private URL url;

    private String credit;

    @Enumerated(EnumType.STRING)
    private Licence licence;

    private URL thumbnailUrl;

    @ManyToMany
    private Set<Metadata> metadata;

    public PublicationKey getId() {
        return id;
    }

    public void setId(PublicationKey id) {
        this.id = id;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public ZonedDateTime getPublicationDateTime() {
        return publicationDateTime;
    }

    public void setPublicationDateTime(ZonedDateTime publicationDateTime) {
        this.publicationDateTime = publicationDateTime;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public Licence getLicence() {
        return licence;
    }

    public void setLicence(Licence licence) {
        this.licence = licence;
    }

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Set<Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(credit, depot, id, licence, metadata, publicationDateTime, thumbnailUrl, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Publication other = (Publication) obj;
        return Objects.equals(credit, other.credit) && Objects.equals(depot, other.depot)
                && Objects.equals(id, other.id) && licence == other.licence && Objects.equals(metadata, other.metadata)
                && Objects.equals(publicationDateTime, other.publicationDateTime)
                && Objects.equals(thumbnailUrl, other.thumbnailUrl) && Objects.equals(url, other.url);
    }
}
