package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;

@Entity
public class DvidsCredit {

    @Id
    private Integer id;

    @Column(nullable = false)
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

    @Override
    public String toString() {
        return "DvidsCredit [name=" + name + ", "
                + (StringUtils.isNotBlank(rank) ? "rank=" + rank + ", " : "") + (url != null ? "url=" + url : "") + "]";
    }
}
