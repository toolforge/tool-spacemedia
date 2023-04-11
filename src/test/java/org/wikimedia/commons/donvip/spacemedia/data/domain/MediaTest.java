package org.wikimedia.commons.donvip.spacemedia.data.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MediaTest {

    private final Media<String, LocalDate> media = new Media<>() {

        private String id;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
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
        media.setId("11290168785");
        media.setTitle("DLR Annual General Assembly / Jahreshauptversammlung &amp; Science Slam 2013");
        assertEquals("DLR Annual General Assembly - Jahreshauptversammlung & Science Slam 2013 (11290168785)",
                media.getUploadTitle());

        media.setId("25254739045");
        media.setTitle(
                "Ein Kolbenkernprobenehmer (piston corer) hängt am Seil neben der Bordwand. Gleich wird er in 1300 mm Tiefe hinabgelassen, um viele Meter in das weiche Sediment auf dem Meeresboden einzudringen und einen entsprechend langen Sedimentkern zu entnehmen.");
        assertEquals(
                "Ein Kolbenkernprobenehmer (piston corer) hängt am Seil neben der Bordwand_ Gleich wird er in 1300 mm Tiefe hinabgelassen, um viele Meter in das weiche Sediment auf dem Meeresboden einzudringen und einen entsprechend lang (25254739045)",
                media.getUploadTitle());
        assertEquals(234, media.getUploadTitle().length());
    }
}
