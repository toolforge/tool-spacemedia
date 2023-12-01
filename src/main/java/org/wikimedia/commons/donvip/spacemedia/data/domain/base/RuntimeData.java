package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RuntimeData {

    @Id
    @Column(nullable = false, length = 50)
    private String orgId;

    @Column(nullable = true)
    private LocalDateTime lastUpdateStart;

    @Column(nullable = true)
    private LocalDateTime lastUpdateEnd;

    @Column(nullable = true)
    private Duration lastUpdateDuration;

    @Column(nullable = true)
    private Long lastUpdateDurationMin;

    @Column(nullable = true)
    private Long lastUpdateDurationWithUploadsMin;

    @Column(nullable = true)
    private Long lastUpdateDurationWithoutUploadsMin;

    @Column(nullable = true)
    private String lastTimestamp;

    /**
     * Date at which older media are simply not fetched anymore to speedup updates
     */
    @Column(nullable = true)
    private LocalDate doNotFetchEarlierThan;

    public RuntimeData() {
        // No-arg constructor required by JPA
    }

    public RuntimeData(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public LocalDateTime getLastUpdateStart() {
        return lastUpdateStart;
    }

    public void setLastUpdateStart(LocalDateTime lastUpdateStart) {
        this.lastUpdateStart = lastUpdateStart;
    }

    public LocalDateTime getLastUpdateEnd() {
        return lastUpdateEnd;
    }

    public void setLastUpdateEnd(LocalDateTime lastUpdateEnd) {
        this.lastUpdateEnd = lastUpdateEnd;
    }

    public Duration getLastUpdateDuration() {
        return lastUpdateDuration;
    }

    public void setLastUpdateDuration(Duration lastUpdateDuration) {
        this.lastUpdateDuration = lastUpdateDuration;
    }

    public Long getLastUpdateDurationMin() {
        return lastUpdateDurationMin;
    }

    public void setLastUpdateDurationMin(Long lastUpdateDurationMin) {
        this.lastUpdateDurationMin = lastUpdateDurationMin;
    }

    public Long getLastUpdateDurationWithUploadsMin() {
        return lastUpdateDurationWithUploadsMin;
    }

    public void setLastUpdateDurationWithUploadsMin(Long lastUpdateDurationWithUploadsMin) {
        this.lastUpdateDurationWithUploadsMin = lastUpdateDurationWithUploadsMin;
    }

    public Long getLastUpdateDurationWithoutUpdatesMin() {
        return lastUpdateDurationWithoutUploadsMin;
    }

    public void setLastUpdateDurationWithoutUploadsMin(Long lastUpdateDurationWithoutUploadsMin) {
        this.lastUpdateDurationWithoutUploadsMin = lastUpdateDurationWithoutUploadsMin;
    }

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(String lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public LocalDate getDoNotFetchEarlierThan() {
        return doNotFetchEarlierThan;
    }

    public void setDoNotFetchEarlierThan(LocalDate doNotFetchEarlierThan) {
        this.doNotFetchEarlierThan = doNotFetchEarlierThan;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, lastUpdateDuration, lastUpdateDurationMin, lastUpdateEnd, lastUpdateStart,
                lastTimestamp, doNotFetchEarlierThan);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RuntimeData other = (RuntimeData) obj;
        return Objects.equals(orgId, other.orgId) && Objects.equals(lastUpdateDuration, other.lastUpdateDuration)
                && Objects.equals(lastUpdateDurationMin, other.lastUpdateDurationMin)
                && Objects.equals(lastUpdateEnd, other.lastUpdateEnd)
                && Objects.equals(lastUpdateStart, other.lastUpdateStart)
                && Objects.equals(lastTimestamp, other.lastTimestamp)
                && Objects.equals(doNotFetchEarlierThan, other.doNotFetchEarlierThan);
    }

    @Override
    public String toString() {
        return "RuntimeData [orgId=" + orgId + ", "
                + (lastUpdateStart != null ? "lastUpdateStart=" + lastUpdateStart + ", " : "")
                + (lastUpdateEnd != null ? "lastUpdateEnd=" + lastUpdateEnd + ", " : "")
                + (lastUpdateDuration != null ? "lastUpdateDuration=" + lastUpdateDuration + ", " : "")
                + (lastUpdateDurationMin != null ? "lastUpdateDurationMin=" + lastUpdateDurationMin + ", " : "")
                + (lastTimestamp != null ? "lastTimestamp=" + lastTimestamp + ", " : "")
                + (doNotFetchEarlierThan != null ? "doNotFetchEarlierThan=" + doNotFetchEarlierThan : "") + "]";
    }
}
