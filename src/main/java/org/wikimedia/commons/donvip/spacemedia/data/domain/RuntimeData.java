package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class RuntimeData {

    @Id
    @Column(nullable = false, length = 50)
    private String agencyId;

    @Column(nullable = true)
    private LocalDateTime lastUpdateStart;

    @Column(nullable = true)
    private LocalDateTime lastUpdateEnd;

    @Column(nullable = true)
    private Duration lastUpdateDuration;

    public RuntimeData() {
        // No-arg constructor required by JPA
    }

    public RuntimeData(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
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

    @Override
    public int hashCode() {
        return Objects.hash(agencyId, lastUpdateDuration, lastUpdateEnd, lastUpdateStart);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RuntimeData other = (RuntimeData) obj;
        return Objects.equals(agencyId, other.agencyId) && Objects.equals(lastUpdateDuration, other.lastUpdateDuration)
                && Objects.equals(lastUpdateEnd, other.lastUpdateEnd)
                && Objects.equals(lastUpdateStart, other.lastUpdateStart);
    }

    @Override
    public String toString() {
        return "RuntimeData [agencyId=" + agencyId + ", lastUpdateStart=" + lastUpdateStart + ", lastUpdateEnd="
                + lastUpdateEnd + ", lastUpdateDuration=" + lastUpdateDuration + "]";
    }
}
