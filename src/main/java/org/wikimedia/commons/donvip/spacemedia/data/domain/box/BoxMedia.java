package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Entity
@Indexed
public class BoxMedia extends SingleFileMedia<BoxMediaId> {

    @Id
    @Embedded
    @DocumentId(identifierBridge = @IdentifierBridgeRef(type = BoxMediaIdBridge.class))
    private BoxMediaId id;

    @Column(nullable = true)
    private String creator;

    public BoxMedia() {

    }

    public BoxMedia(String app, String share, long id) {
        setId(new BoxMediaId(app, share, id));
    }

    @Override
    public BoxMediaId getId() {
        return id;
    }

    @Override
    public void setId(BoxMediaId id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    protected String getUploadId(FileMetadata fileMetadata) {
        return Long.toString(getId().getId());
    }

    @Override
    protected String getUploadTitle() {
        int idx = title.lastIndexOf('.');
        String result = CommonsService.normalizeFilename(idx > -1 ? title.substring(0, idx) : title);
        return result.replace("IMG_", "").matches("[\\p{Alnum}_]+") ? id.getApp() + ' ' + result : result;
    }

    public BoxMedia copyDataFrom(BoxMedia other) {
        super.copyDataFrom(other);
        this.creator = other.creator;
        return this;
    }

    @Override
    public String toString() {
        return "BoxMedia [" + (id != null ? "id=" + id + ", " : "")
                + (creator != null ? "creator=" + creator + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + "metadata=" + getMetadata() + ']';
    }
}
