package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.util.Locale;
import java.util.Set;

public enum FileFormat {
    GIF(ImageFile.class, "image/gif"),
    JPEG(ImageFile.class, "image/jpeg", "JPG"),
    MP4(VideoFile.class, "video/mp4"),
    PDF(PdfFile.class, "application/pdf"),
    PNG(ImageFile.class, "image/png"),
    TIFF(ImageFile.class, "image/tiff", "TIF"),
    WEBA(AudioFile.class, "audio/webm"),
    WEBM(VideoFile.class, "video/webm"),
    WEBP(ImageFile.class, "image/webp");

    private final Class<? extends File> fileClass;

    private final String contentType;

    private final Set<String> additionalExtensions;

    private FileFormat(Class<? extends File> fileClass, String contentType, String... additionalExtensions) {
        this.fileClass = fileClass;
        this.contentType = contentType;
        this.additionalExtensions = Set.of(additionalExtensions);
    }

    public static FileFormat fromContentType(String contentType) {
        for (FileFormat ff : values()) {
            if (ff.contentType.equals(contentType)) {
                return ff;
            }
        }
        return null;
    }

    public static FileFormat fromExtension(String ext) {
        String extUC = ext.toUpperCase(Locale.ENGLISH);
        for (FileFormat ff : values()) {
            if (extUC.equals(ff.name()) || ff.additionalExtensions.contains(extUC)) {
                return ff;
            }
        }
        return null;
    }

    public Class<? extends File> getFileClass() {
        return fileClass;
    }

    public String getContentType() {
        return contentType;
    }

    public Set<String> getAdditionalExtensions() {
        return additionalExtensions;
    }
}
