package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciImageFiles;

class StsciServiceTest {

    @Test
    void testFileDownloadTextPattern() throws IOException {
        doTest("7400 X 4162, PNG (20.44 MB)", 7400, 4162, 21432893);
        doTest("4684 X 2807, TIF (21.41 MB)", 4684, 2807, 22450012);
        doTest("Text Description, PDF (90.65 KB)", 0, 0, 92825);
        doTest("Full Res, 3840 X 2160, TIF (8.30 MB)", 3840, 2160, 8703180);
        doTest("Annotated, 1666 X 1332, TIF (12.74 MB)", 1666, 1332, 13358858);
        doTest("5217 X 2499, TIFF (20.53 MB)", 5217, 2499, 21527265);
        doTest("Annotated Medium, 1604 X 2000, PNG (5.58 MB)", 1604, 2000, 5851054);
        doTest("PDF (85.62 KB)", 0, 0, 87674);
    }

    private static void doTest(String text, int width, int height, int sizeInBytes) throws IOException {
        StsciImageFiles file = StsciService.extractFile(null, null, text);
        assertNotNull(file);
        assertEquals(width, file.getWidth());
        assertEquals(height, file.getHeight());
        assertEquals(sizeInBytes, file.getFileSize());
    }
}
