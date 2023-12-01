package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Categorylinks_table">Mediawiki
 * Category Links table</a>
 * 
 * <pre>
 * +-------------------+------------------------------+------+-----+-------------------+-----------------------------+
 * | Field             | Type                         | Null | Key | Default           | Extra                       |
 * +-------------------+------------------------------+------+-----+-------------------+-----------------------------+
 * | cl_from           | int(10) unsigned             | NO   | PRI | 0                 |                             |
 * | cl_to             | varbinary(255)               | NO   | PRI |                   |                             |
 * | cl_sortkey        | varbinary(230)               | NO   |     |                   |                             |
 * | cl_sortkey_prefix | varbinary(255)               | NO   |     |                   |                             |
 * | cl_timestamp      | timestamp                    | NO   |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
 * | cl_collation      | varbinary(32)                | NO   | MUL |                   |                             |
 * | cl_type           | enum('page','subcat','file') | NO   |     | page              |                             |
 * +-------------------+------------------------------+------+-----+-------------------+-----------------------------+
 * </pre>
 */
@Entity
@Table(name = "categorylinks")
public class CommonsCategoryLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private CommonsCategoryLinkId id;

    @Column(name = "cl_sortkey", nullable = false, length = 230, columnDefinition = "VARBINARY")
    private String sortkey;

    @Column(name = "cl_sortkey_prefix", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String sortkeyPrefix;

    @Column(name = "cl_timestamp", nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private LocalDateTime timestamp;

    @Column(name = "cl_collation", nullable = false, length = 32, columnDefinition = "VARBINARY")
    private String collation;

    @Enumerated(EnumType.STRING)
    @Column(name = "cl_type", nullable = false, length = 32, columnDefinition = "enum('page','subcat','file') default 'page'")
    private CommonsCategoryLinkType type;

    public CommonsCategoryLinkId getId() {
        return id;
    }

    public void setId(CommonsCategoryLinkId id) {
        this.id = id;
    }

    public String getSortkey() {
        return sortkey;
    }

    public void setSortkey(String sortkey) {
        this.sortkey = sortkey;
    }

    public String getSortkeyPrefix() {
        return sortkeyPrefix;
    }

    public void setSortkeyPrefix(String sortkeyPrefix) {
        this.sortkeyPrefix = sortkeyPrefix;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    public CommonsCategoryLinkType getType() {
        return type;
    }

    public void setType(CommonsCategoryLinkType type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsCategoryLink other = (CommonsCategoryLink) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return "CommonsCategoryLink [id=" + id + ", sortkey=" + sortkey + ", sortkeyPrefix=" + sortkeyPrefix
                + ", timestamp=" + timestamp + ", collation=" + collation + ", type=" + type + "]";
    }
}
