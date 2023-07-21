package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import java.time.ZonedDateTime;

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
public class BoxMedia extends SingleFileMedia<BoxMediaId, ZonedDateTime> {

    @Id
    @Embedded
    @DocumentId(identifierBridge = @IdentifierBridgeRef(type = BoxMediaIdBridge.class))
    private BoxMediaId id;

    @Column(nullable = true)
    private ZonedDateTime contentCreationDate;

    @Column(nullable = false)
    private ZonedDateTime date;

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

    public ZonedDateTime getContentCreationDate() {
        return contentCreationDate;
    }

    public void setContentCreationDate(ZonedDateTime contentCreationDate) {
        this.contentCreationDate = contentCreationDate;
    }

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(ZonedDateTime creationDate) {
        this.date = creationDate;
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
        return CommonsService.normalizeFilename(idx > -1 ? title.substring(0, idx) : title);
    }

    public BoxMedia copyDataFrom(BoxMedia other) {
        super.copyDataFrom(other);
        this.contentCreationDate = other.contentCreationDate;
        this.creator = other.creator;
        return this;
    }

    @Override
    public String toString() {
        return "BoxMedia [" + (id != null ? "id=" + id + ", " : "")
                + (contentCreationDate != null ? "contentCreationDate=" + contentCreationDate + ", " : "")
                + (date != null ? "date=" + date + ", " : "") + (creator != null ? "creator=" + creator + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + "metadata=" + getMetadata() + ']';
    }
}
