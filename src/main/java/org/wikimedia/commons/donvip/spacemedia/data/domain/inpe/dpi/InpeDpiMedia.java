package org.wikimedia.commons.donvip.spacemedia.data.domain.inpe.dpi;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

import jakarta.persistence.Entity;

@Entity
public class InpeDpiMedia extends Media {

    @Override
    public String getUploadTitle(FileMetadata fileMetadata) {
        String fileName = fileMetadata.getOriginalFileName();
        if (fileName != null) {
            fileName = fileName.substring(0, fileName.indexOf('.')).replace('_', ' ').trim();
        }
        return fileName != null && !isTitleBlacklisted(fileName) ? fileName : super.getUploadTitle(fileMetadata);
    }

    @Override
    public String toString() {
        return "InpeDpiMedia [id=" + getId() + ']';
    }

    public InpeDpiMedia copyDataFrom(InpeDpiMedia media) {
        super.copyDataFrom(media);
        return this;
    }
}
