package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Entity
@Table(indexes = { @Index(columnList = "sha1, full_res_sha1, phash, full_res_phash") })
public class EsaMedia extends FullResMedia<Integer, LocalDateTime> {

    @Id
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false, unique = true, length = 200)
    private URL url;
    @Column(name = "released", nullable = false)
    private LocalDateTime date;
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

    @Override
    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
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

    public Set<String> getKeywords() {
        return keywords;
    }

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
        return "EsaMedia [id=" + id + ", " + (url != null ? "url=" + url + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (date != null ? "released=" + date + ", " : "")
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
    public String getUploadTitle() {
        return CommonsService.normalizeFilename(title) + " ESA" + getUploadId();
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return true;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public boolean considerVariants() {
        return "Mars Express".equals(mission) || "BepiColombo".equals(mission);
    }
}
