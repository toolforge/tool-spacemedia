package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(indexes = { @Index(columnList = "url") })
public class EsaMedia extends Media implements WithKeywords {

    @Column(nullable = false, unique = true, length = 200)
    private URL url;
    @Column(length = 64)
    private String activity;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String mission;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String people;
    @Column(length = 48)
    private String action;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> systems = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> locations = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();
    @Column(length = 70)
    private String photoSet;
    @Lob
    @Column(columnDefinition = "TEXT", nullable = true)
    private String licence;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getPeople() {
        return people;
    }

    public void setPeople(String people) {
        this.people = people;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Set<String> getSystems() {
        return systems;
    }

    public void setSystems(Set<String> systems) {
        this.systems = systems;
    }

    public Set<String> getLocations() {
        return locations;
    }

    public void setLocations(Set<String> locations) {
        this.locations = locations;
    }

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public String getPhotoSet() {
        return photoSet;
    }

    public void setPhotoSet(String photoSet) {
        this.photoSet = photoSet;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    @Override
    public List<String> getAlbumNames() {
        return photoSet != null ? List.of(photoSet) : List.of();
    }

    @Override
    public String toString() {
        return "EsaMedia [id=" + getId() + ", " + (url != null ? "url=" + url + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (activity != null ? "activity=" + activity + ", " : "")
                + (mission != null ? "mission=" + mission + ", " : "")
                + (people != null ? "people=" + people + ", " : "") + (action != null ? "action=" + action + ", " : "")
                + (systems != null ? "systems=" + systems + ", " : "")
                + (locations != null ? "locations=" + locations + ", " : "")
                + (keywords != null ? "keywords=" + keywords + ", " : "")
                + (photoSet != null ? "photoSet=" + photoSet : "") + "]";
    }

    @Override
    public String getUploadTitle(FileMetadata fileMetadata) {
        return CommonsService.normalizeFilename(title) + " ESA" + getUploadId(fileMetadata);
    }

    public EsaMedia copyDataFrom(EsaMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.url = mediaFromApi.url;
        this.activity = mediaFromApi.activity;
        this.mission = mediaFromApi.mission;
        this.people = mediaFromApi.people;
        this.action = mediaFromApi.action;
        this.licence = mediaFromApi.licence;
        return this;
    }
}
