package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class Depot {

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private URL website;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Person> operators = new HashSet<>();

    public Depot() {

    }

    public Depot(String id, String name, URL website, Set<? extends Person> operators) {
        this.id = id;
        this.name = name;
        this.website = website;
        this.operators.addAll(operators);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    public Set<Person> getOperators() {
        return operators;
    }

    public void setOperators(Set<Person> operators) {
        this.operators = operators;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, website);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Depot other = (Depot) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name)
                && Objects.equals(website, other.website);
    }

    @Override
    public String toString() {
        return id;
    }
}
