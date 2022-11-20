package org.wikimedia.commons.donvip.spacemedia.data.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MediaTest {

    private final Media<String, LocalDate> media = new Media<>() {

        @Override
        public String getId() {
            return "11290168785";
        }

        @Override
        public void setId(String id) {
        }

        @Override
        public LocalDate getDate() {
            return null;
        }

        @Override
        public void setDate(LocalDate date) {
        }

        @Override
        public boolean isAudio() {
            return false;
        }

        @Override
        public boolean isImage() {
            return false;
        }

        @Override
        public boolean isVideo() {
            return false;
        }
    };

    @Test
    void testGetUploadTitle() {
        media.setTitle("DLR Annual General Assembly / Jahreshauptversammlung &amp; Science Slam 2013");
        assertEquals("DLR Annual General Assembly / Jahreshauptversammlung & Science Slam 2013 (11290168785)",
                media.getUploadTitle());
    }
}
