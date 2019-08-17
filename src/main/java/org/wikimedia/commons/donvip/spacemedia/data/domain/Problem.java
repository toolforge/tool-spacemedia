package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Problem {

    @Id
    @Column(nullable = false)
    @GeneratedValue
    private Integer id;

    @Column(nullable = false)
    private String agency;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, length = 380)
    private URL problematicUrl;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public URL getProblematicUrl() {
        return problematicUrl;
    }

    public void setProblematicUrl(URL problematicUrl) {
        this.problematicUrl = problematicUrl;
    }

    @Override
    public String toString() {
        return "Problem [" + (agency != null ? "agency=" + agency + ", " : "")
                + (errorMessage != null ? "errorMessage=" + errorMessage + ", " : "")
                + (problematicUrl != null ? "problematicUrl=" + problematicUrl : "") + "]";
    }
}
