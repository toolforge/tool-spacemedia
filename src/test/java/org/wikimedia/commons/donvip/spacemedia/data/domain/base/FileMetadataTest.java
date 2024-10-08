package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FileMetadataTest {

    @Test
    void testHasValidDimensions() {
        FileMetadata metadata = new FileMetadata();
        assertFalse(metadata.hasValidDimensions());
        metadata.setMediaDimensions(new MediaDimensions());
        assertFalse(metadata.hasValidDimensions());
        metadata.getMediaDimensions().setWidth(0);
        assertFalse(metadata.hasValidDimensions());
        metadata.getMediaDimensions().setHeight(0);
        assertFalse(metadata.hasValidDimensions());
        metadata.getMediaDimensions().setWidth(1);
        assertFalse(metadata.hasValidDimensions());
        metadata.getMediaDimensions().setHeight(1);
        assertTrue(metadata.hasValidDimensions());
    }

    @Test
    void testConstructor() throws Exception {
        FileMetadata fm = new FileMetadata("https://www.flickr.com/video_download.gne?id=53199053407");
        assertNull(fm.getOriginalFileName());
        assertNull(fm.getFileExtension());
        assertEquals("https://www.flickr.com/video_download.gne?id=53199053407", fm.getAssetUri().toString());

        fm = new FileMetadata(new URL("https://www.kari.re.kr/image/kari_image_down.do?idx=19"));
        assertNull(fm.getOriginalFileName());
        assertNull(fm.getFileExtension());

        fm = new FileMetadata("http://images-assets.nasa.gov/image/NHQ202304250006/NHQ202304250006~orig.jpg");
        assertEquals("NHQ202304250006~orig.jpg", fm.getOriginalFileName());
        assertEquals("jpg", fm.getFileExtension());
    }

    @Test
    void testIsImage() {
        FileMetadata fm = new FileMetadata();
        for (String ext : Set.of("bmp", "jpg", "jpeg", "tif", "tiff", "png", "webp", "xcf", "gif", "svg")) {
            fm.setExtension(ext);
            assertTrue(fm.isImage());
        }
    }
}
