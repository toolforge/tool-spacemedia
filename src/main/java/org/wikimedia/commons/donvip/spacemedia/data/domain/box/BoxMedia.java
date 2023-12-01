package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Entity
@Indexed
public class BoxMedia extends SingleFileMedia {

    @Column(nullable = true)
    private String creator;

    public BoxMedia() {

    }

    public BoxMedia(String app, String share, long id) {
        setId(new CompositeMediaId(app + '/' + share, Long.toString(id)));
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    protected String getUploadTitle() {
        int idx = title.lastIndexOf('.');
        String result = CommonsService.normalizeFilename(idx > -1 ? title.substring(0, idx) : title);
        return result.replace("IMG_", "").matches("[\\p{Alnum}_]+") ? getId().getRepoId().split("/")[0] + ' ' + result
                : result;
    }

    public BoxMedia copyDataFrom(BoxMedia other) {
        super.copyDataFrom(other);
        this.creator = other.creator;
        return this;
    }

    @Override
    public String toString() {
        return "BoxMedia [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (creator != null ? "creator=" + creator + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + "metadata=" + getMetadata() + ']';
    }
}
