package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.Objects;

import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;

@MappedSuperclass
public abstract class DefaultMedia extends Media<CompositeMediaId> {

    @Id
    @Embedded
    @DocumentId(identifierBridge = @IdentifierBridgeRef(type = CompositeMediaIdBridge.class))
    private CompositeMediaId id;

    @Override
    public CompositeMediaId getId() {
        return id;
    }

    @Override
    public void setId(CompositeMediaId id) {
        this.id = id;
    }

    @Override
    public String getIdUsedInOrg() {
        return getId().getMediaId();
    }

    @Override
    protected String getUploadId(FileMetadata fileMetadata) {
        return getId().getMediaId();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        DefaultMedia other = (DefaultMedia) obj;
        return Objects.equals(id, other.id);
    }
}
