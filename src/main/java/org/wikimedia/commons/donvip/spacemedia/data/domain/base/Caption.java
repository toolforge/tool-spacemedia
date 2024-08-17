package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static jakarta.persistence.GenerationType.SEQUENCE;

import java.net.URL;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = { @Index(columnList = "url") })
public class Caption {
    
    @Id
    @JsonIgnore
    @GeneratedValue(strategy = SEQUENCE, generator = "caption_sequence")
    private Long id;

    @Column(nullable = false, length = 8)
    private String lang;

    @Column(nullable = false, length = 540)
    private URL url;

    public Caption() {
    }

    public Caption(String lang, URL url) {
        this.lang = lang;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lang, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Caption other = (Caption) obj;
        return Objects.equals(id, other.id) && Objects.equals(lang, other.lang) && Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return "Caption [id=" + id + ", lang=" + lang + ", url=" + url + "]";
    }
}
