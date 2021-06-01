package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

@MappedSuperclass
public abstract class File {

    /**
     * SHA-1 hash.
     */
    @Id
    @Column(nullable = false, length = 42)
    private String sha1;

    /**
     * File size in bytes.
     */
    @Column(nullable = false)
    private Long size;

    @ManyToMany
    private Set<Metadata> metadata;

    @OneToMany
    private Set<CommonsFile> commonsFiles;

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public Set<Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }

    public Set<CommonsFile> getCommonsFiles() {
        return commonsFiles;
    }

    public void setCommonsFiles(Set<CommonsFile> commonsFiles) {
        this.commonsFiles = commonsFiles;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonsFiles, metadata, sha1, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        File other = (File) obj;
        return Objects.equals(commonsFiles, other.commonsFiles) && Objects.equals(metadata, other.metadata)
                && Objects.equals(sha1, other.sha1) && Objects.equals(size, other.size);
    }
}
