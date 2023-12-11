package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;

class ErccMediaTest {

    @Test
    void testGetUploadTitle() {
        ErccMedia media = new ErccMedia();
        media.setId(new CompositeMediaId("Situation", "3484"));
        media.setTitle("BOSNIA AND HERZEGOVINA – Increased arrivals and presence of refugees and migrants");
        FileMetadata fm = new FileMetadata();
        fm.setOriginalFileName("image.png");
        media.addMetadata(fm);

        assertEquals("BOSNIA AND HERZEGOVINA – Increased arrivals and presence of refugees and migrants (3484)",
                media.getUploadTitle(fm));
    }
}
