package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Hearbeat table, see
 * https://wikitech.wikimedia.org/wiki/Help:Toolforge/Database#Identifying_lag
 */
@Entity
@Table(name = "heartbeat", catalog = "heartbeat_p")
public class Heartbeat {

    @Id
    @Column(name = "shard")
    private String shard;

    @Column(name = "last_updated")
    private String lastUpdated;

    @Column(name = "lag")
    private double lag;

    public String getShard() {
        return shard;
    }

    public void setShard(String shard) {
        this.shard = shard;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public double getLag() {
        return lag;
    }

    public void setLag(double lag) {
        this.lag = lag;
    }

    @Override
    public String toString() {
        return "Heartbeat [shard=" + shard + ", lastUpdated=" + lastUpdated + ", lag=" + lag + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(lag, lastUpdated, shard);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Heartbeat other = (Heartbeat) obj;
        return Double.doubleToLongBits(lag) == Double.doubleToLongBits(other.lag)
                && Objects.equals(lastUpdated, other.lastUpdated) && Objects.equals(shard, other.shard);
    }
}
