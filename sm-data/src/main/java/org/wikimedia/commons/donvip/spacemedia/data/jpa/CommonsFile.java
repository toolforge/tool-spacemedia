package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CommonsFile {

    @Id
    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsFile other = (CommonsFile) obj;
        return Objects.equals(filename, other.filename);
    }
}
