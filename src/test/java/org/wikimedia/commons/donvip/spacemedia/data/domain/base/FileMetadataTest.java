package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileMetadataTest {

    @Test
    void testHasValidDImensions() {
        FileMetadata metadata = new FileMetadata();
        assertFalse(metadata.hasValidDimensions());
        metadata.setImageDimensions(new ImageDimensions());
        assertFalse(metadata.hasValidDimensions());
        metadata.getImageDimensions().setWidth(0);
        assertFalse(metadata.hasValidDimensions());
        metadata.getImageDimensions().setHeight(0);
        assertFalse(metadata.hasValidDimensions());
        metadata.getImageDimensions().setWidth(1);
        assertFalse(metadata.hasValidDimensions());
        metadata.getImageDimensions().setHeight(1);
        assertTrue(metadata.hasValidDimensions());
    }

    @Test
    void testConstructor() throws Exception {
        FileMetadata fm = new FileMetadata("https://www.flickr.com/video_download.gne?id=53199053407");
        assertEquals("video_download.gne", fm.getOriginalFileName());
        assertNull(fm.getFileExtension());
        assertEquals("https://www.flickr.com/video_download.gne?id=53199053407", fm.getAssetUri().toString());
    }
}
