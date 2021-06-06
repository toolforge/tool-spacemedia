package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;

@MappedSuperclass
public abstract class Publication {

    @EmbeddedId
    private PublicationKey id;

    @MapsId("depot_id")
    @ManyToOne
    private Depot depot;

    private ZonedDateTime publicationDateTime;

    @Column(nullable = false, unique = true, length = 320)
    private URL url;

    @Column(nullable = true, length = 460)
    private String credit;

    @Enumerated(EnumType.STRING)
    private Licence licence;

    @Column(nullable = true, length = 320)
    private URL thumbnailUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Metadata> metadata = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Person> authors = new HashSet<>();

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

    public Set<String> getMetadataValues(String key) {
        return getMetadata().stream().filter(m -> m.getKey().equals(key)).map(Metadata::getValue)
                .collect(Collectors.toSet());
    }

    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }

    public boolean addMetadata(Metadata md) {
        return metadata.add(md);
    }

    public Set<Person> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Person> authors) {
        this.authors = authors;
    }

    public boolean addAuthor(Person person) {
        return authors.add(person);
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
