package org.wikimedia.commons.donvip.spacemedia.data.local;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
public class Problem {

    @Id
    @NotNull
    @GeneratedValue
    private Integer id;

    @NotNull
    private String agency;

    @Column(length = 500)
    private String errorMessage;

    @NotNull
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
