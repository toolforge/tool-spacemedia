package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static javax.persistence.GenerationType.SEQUENCE;

import java.net.URL;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class Problem {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = SEQUENCE, generator = "problem_sequence")
    private Integer id;

    /**
     * Org identifier.
     */
    @Column(nullable = false)
    private String org;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
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
     * Returns the org identifier.
     *
     * @return the org identifier
     */
    public String getOrg() {
        return org;
    }

    /**
     * Sets the org identifier.
     *
     * @param org the org identifier
     */
    public void setOrg(String org) {
        this.org = org;
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
        return "Problem [" + (org != null ? "org=" + org + ", " : "")
                + (errorMessage != null ? "errorMessage=" + errorMessage + ", " : "")
                + (problematicUrl != null ? "problematicUrl=" + problematicUrl : "") + "]";
    }
}
