package org.wikimedia.commons.donvip.spacemedia.service.stsci;

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
        doTest("NIRCam Only, Full Res, 12654 X 12132, TIF (160.01 MB)", 12654, 12132, 167782645);
        doTest("Text Description of Webb Diffraction Spikes Infographic, PDF (37.56 KB)", 0, 0, 38461);
        doTest("Full Res., 1920 X 1080, PNG (450.31 KB)", 1920, 1080, 461117);
        doTest("Vertical Version, Full Res, 6900 X 10809, PNG (2.35 MB)", 6900, 10809, 2464153);
        doTest("Full identifier, transparent background, 555 X 309, PNG (28.96 KB)", 555, 309, 29655);
        doTest("Full Res (Annotated), 4000 X 2000, PNG (2.00 MB)", 4000, 2000, 2097152);
        doTest("Text-editable PDF, PDF (604.28 KB)", 0, 0, 618782);
        doTest("Half Res [FOR DOWNLOAD ONLY], 16037 X 20574, PNG (542.52 MB)", 16037, 20574, 568873451);
        doTest("MACS J1341, 1750 X 1750, TIF (7.59 MB)", 1750, 1750, 7958691);
        doTest("2000x1125, 2000 X 1125, PNG (993.39 KB)", 2000, 1125, 1017231);
    }

    private static void doTest(String text, int width, int height, int sizeInBytes) throws IOException {
        StsciImageFiles file = StsciService.extractFile(null, null, text);
        assertNotNull(file);
        assertEquals(width, file.getWidth());
        assertEquals(height, file.getHeight());
        assertEquals(sizeInBytes, file.getFileSize());
    }
}
