package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.Table;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Entity
@Table(indexes = { @Index(columnList = "url") })
public class EsaMedia extends Media implements WithKeywords {

    @Column(nullable = false, unique = true, length = 200)
    private URL url;
    @Column(nullable = false, length = 300)
    private String copyright;
    @Column(length = 64)
    private String activity;
    @Column(length = 60)
    private String mission;
    @Column(length = 70)
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

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
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

    @Override
    public String toString() {
        return "EsaMedia [id=" + getId() + ", " + (url != null ? "url=" + url + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (copyright != null ? "copyright=" + copyright + ", " : "")
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
        this.copyright = mediaFromApi.copyright;
        this.activity = mediaFromApi.activity;
        this.mission = mediaFromApi.mission;
        this.people = mediaFromApi.people;
        this.action = mediaFromApi.action;
        return this;
    }
}
