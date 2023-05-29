package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
