package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.time.LocalDateTime;

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

    /**
     * Agency identifier.
     */
    @Column(nullable = false)
    private String agency;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, length = 380)
    private URL problematicUrl;

    @Column(nullable = false)
    private LocalDateTime date;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Returns the agency identifier.
     *
     * @return the agency identifier
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Sets the agency identifier.
     *
     * @param agency the agency identifier
     */
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

    /**
     * Returns the date at which the problem occurred.
     *
     * @return the date at which the problem occurred
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Sets the date at which the problem occurred.
     *
     * @param date the date at which the problem occurred
     */
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Problem [" + (agency != null ? "agency=" + agency + ", " : "")
                + (errorMessage != null ? "errorMessage=" + errorMessage + ", " : "")
                + (problematicUrl != null ? "problematicUrl=" + problematicUrl : "") + "]";
    }
}
