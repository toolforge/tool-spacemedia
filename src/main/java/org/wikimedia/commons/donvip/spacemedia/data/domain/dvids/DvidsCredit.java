package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class DvidsCredit {

    private Integer id;

    private String name;

    private String rank;

    private URL url;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String dvidsCreditToString() {
        StringBuilder result = new StringBuilder();
        if (StringUtils.isNotBlank(getRank())) {
            result.append(getRank()).append(' ');
        }
        result.append('[').append(getUrl()).append(' ').append(getName()).append(']');
        return result.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, rank, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DvidsCredit other = (DvidsCredit) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(rank, other.rank)
                && Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return "DvidsCredit [name=" + name + ", "
                + (StringUtils.isNotBlank(rank) ? "rank=" + rank + ", " : "") + (url != null ? "url=" + url : "") + ']';
    }
}
