package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class CompositeMediaId implements Serializable, Comparable<CompositeMediaId> {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 64)
    private String repoId;

    @Column(nullable = false, length = 512)
    private String mediaId;

    public CompositeMediaId() {

    }

    public CompositeMediaId(String repoId, String mediaId) {
        this.repoId = requireNonNull(repoId, "repoId");
        this.mediaId = requireNonNull(mediaId, "mediaId");
    }

    public CompositeMediaId(String jsonId) {
        int idx = jsonId.indexOf(':');
        this.repoId = jsonId.substring(0, idx);
        this.mediaId = jsonId.substring(idx + 1);
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoId, mediaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CompositeMediaId other = (CompositeMediaId) obj;
        return Objects.equals(repoId, other.repoId) && Objects.equals(mediaId, other.mediaId);
    }

    @Override
    public int compareTo(CompositeMediaId o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return repoId + ':' + mediaId;
    }
}
