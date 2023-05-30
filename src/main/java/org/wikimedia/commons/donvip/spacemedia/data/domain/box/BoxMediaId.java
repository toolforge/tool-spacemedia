package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class BoxMediaId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 32)
    private String app;

    @Column(nullable = false, length = 32)
    private String share;

    private long id;

    public BoxMediaId() {

    }

    public BoxMediaId(String app, String share, long id) {
        this.app = app;
        this.share = share;
        this.id = id;
    }

    public BoxMediaId(String jsonId) {
        String[] tab = jsonId.split(":");
        this.app = tab[0];
        this.share = tab[1];
        this.id = Long.parseLong(tab[2]);
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getShare() {
        return share;
    }

    public void setShare(String sharedName) {
        this.share = sharedName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(app, share, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        BoxMediaId other = (BoxMediaId) obj;
        return Objects.equals(app, other.app) && Objects.equals(id, other.id) && Objects.equals(share, other.share);
    }

    @Override
    public String toString() {
        return app + ':' + share + ':' + id;
    }
}
