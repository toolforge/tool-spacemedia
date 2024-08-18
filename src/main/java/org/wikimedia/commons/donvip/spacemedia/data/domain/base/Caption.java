package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Embeddable
@Table(indexes = { @Index(columnList = "url") })
public class Caption {

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
        return Objects.hash(lang, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Caption other = (Caption) obj;
        return Objects.equals(lang, other.lang) && Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return "Caption [lang=" + lang + ", url=" + url + "]";
    }
}
