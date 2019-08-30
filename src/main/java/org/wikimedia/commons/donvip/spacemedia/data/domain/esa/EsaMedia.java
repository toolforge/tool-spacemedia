package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;

@Entity
public class EsaMedia extends FullResMedia<Integer> {

    @Id
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false, unique = true, length = 200)
    private URL url;
    private LocalDateTime released;
    @Column(nullable = false, length = 300)
    private String copyright;
    @Column(length = 70)
    private String activity;
    @Column(length = 60)
    private String mission;
    @Column(length = 80)
    private String people;
    @Column(length = 50)
    private String action;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> systems;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> locations;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords;
    @Column(length = 80)
    private String photoSet;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public LocalDateTime getReleased() {
        return released;
    }

    public void setReleased(LocalDateTime released) {
        this.released = released;
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
                + (released != null ? "released=" + released + ", " : "")
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
}
