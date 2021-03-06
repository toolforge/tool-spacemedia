package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Abstract superclass of persons (organizations and humans).
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(value = "P")
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
