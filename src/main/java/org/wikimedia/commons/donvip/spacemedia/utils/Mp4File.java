package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

import org.mp4parser.Box;
import org.mp4parser.BoxParser;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MediaDataBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://github.com/sannies/mp4parser/issues/426
public class Mp4File extends IsoFile {
    private static final Logger LOG = LoggerFactory.getLogger(Mp4File.class);

    public Mp4File(File file) throws IOException {
        super(file);
    }

    public Mp4File(ReadableByteChannel readableByteChannel, BoxParser boxParser) throws IOException {
        super(readableByteChannel, boxParser);
    }

    public Mp4File(ReadableByteChannel readableByteChannel) throws IOException {
        super(readableByteChannel);
    }

    public Mp4File(String file) throws IOException {
        super(file);
    }

    @Override
    public void close() throws IOException {
        for (Box box : getBoxes()) {
            if (box instanceof Closeable closeableBox) {
                closeableBox.close();
            } else if (box instanceof MediaDataBox mediaDataBox) {
                try {
                    Field f = mediaDataBox.getClass().getDeclaredField("dataFile");
                    f.setAccessible(true); // NOSONAR
                    File dataFile = (File) f.get(mediaDataBox);
                    try {
                        Files.delete(dataFile.toPath());
                    } catch (IOException e) {
                        LOG.warn("failed to delete: {}. Delete it on exit.", dataFile);
                        dataFile.deleteOnExit();
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        super.close();
    }
}
