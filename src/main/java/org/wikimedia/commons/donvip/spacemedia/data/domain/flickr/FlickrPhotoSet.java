package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
public class FlickrPhotoSet {

    @Id
    @Column(nullable = false)
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(nullable = false)
    private String pathAlias;

    @ManyToMany
    @JsonIgnoreProperties("photosets")
    protected Set<FlickrMedia> members = new HashSet<>();

    public FlickrPhotoSet() {

    }

    public FlickrPhotoSet(Long id, String title) {
        setId(id);
        setTitle(title);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<FlickrMedia> getMembers() {
        return members;
    }

    public void setMembers(Set<FlickrMedia> members) {
        this.members = members;
    }

    public String getPathAlias() {
        return pathAlias;
    }

    public void setPathAlias(String pathAlias) {
        this.pathAlias = pathAlias;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        FlickrPhotoSet other = (FlickrPhotoSet) obj;
        return Objects.equals(id, other.id) && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return title;
    }
}
