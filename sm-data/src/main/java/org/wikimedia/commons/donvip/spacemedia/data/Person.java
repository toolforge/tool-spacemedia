package org.wikimedia.commons.donvip.spacemedia.data;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Abstract superclass of persons (organizations and humans).
 */
@MappedSuperclass
public abstract class Person {

    /**
     * Unique name identifying the person.
     */
    @Id
    @Column(nullable = false)
    protected String name;

    /**
     * Optional Wikidata Q identifier (should be defined for most organizations, but few humans)
     */
    @Column(nullable = true)
    protected String wikidata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWikidata() {
        return wikidata;
    }

    public void setWikidata(String wikidata) {
        this.wikidata = wikidata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, wikidata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        return Objects.equals(name, other.name) && Objects.equals(wikidata, other.wikidata);
    }
}
