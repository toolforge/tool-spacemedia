package org.wikimedia.commons.donvip.spacemedia.data.local;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Media {
    @Column(nullable = false, length = 42)
    protected String sha1;

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> commonsFileNames;
    
    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public Set<String> getCommonsFileNames() {
        return commonsFileNames;
    }

    public void setCommonsFileNames(Set<String> commonsFileNames) {
        this.commonsFileNames = commonsFileNames;
    }
}
