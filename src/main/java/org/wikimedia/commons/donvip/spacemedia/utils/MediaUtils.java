package org.wikimedia.commons.donvip.spacemedia.utils;

import static com.drew.metadata.file.FileSystemDirectory.TAG_FILE_SIZE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.execOutput;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.executeRequest;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.findExtension;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpHead;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.util.PPTX2PNG;
import org.gagravarr.ogg.OggFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDimensions;
import org.wikimedia.commons.donvip.spacemedia.exception.FileDecodingException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.file.FileSystemDirectory;

public class MediaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUtils.class);

    private static final Set<String> IMAGEIO_EXTENSIONS = Set.of("bmp", "gif", "jpe", "jpg", "jpeg", "png", "svg",
            "tif", "tiff");

    private static final Set<String> POI_HSLF_EXTENSIONS = Set.of("ppt");
    private static final Set<String> POI_XSLF_EXTENSIONS = Set.of("pptm", "pptx");

    private static final Pattern MERGING_DL = Pattern
            .compile("\\[ffmpeg\\] Merging formats into \"(\\S{11}\\.\\S{3,4})\"");
    private static final Pattern ALREADY_DL = Pattern
            .compile("\\[download\\] (\\S{11}\\.\\S{3,4}) has already been downloaded(?: and merged)?");
    private static final Pattern DESTINATION = Pattern.compile("\\[download\\] Destination: (\\S{11}\\.\\S{3,4})");

    private MediaUtils() {
        // Hide default constructor
    }

    public static <T> ContentsAndMetadata<T> readFile(URL url, String extension, Path localPath, boolean readMetadata,
            boolean log, HttpClient httpClient, HttpClientContext context) throws IOException, FileDecodingException {
        return readFile(urlToUriUnchecked(url), extension, localPath, readMetadata, log, httpClient, context);
    }

    public static <T> ContentsAndMetadata<T> readFile(URI uri, String extension, Path localPath, boolean readMetadata,
            boolean log, HttpClient httpClient, HttpClientContext context) throws IOException, FileDecodingException {
        if (log) {
            LOGGER.info("Reading file {}", uri);
        }
        if (isBlank(extension)) {
            extension = findExtension(uri.toString());
        }
        try (ClassicHttpResponse response = executeRequest(newHttpHead(uri), httpClient, context)) {
            Header header = response.getHeader(HttpHeaders.CONTENT_LENGTH);
            if (header != null) {
                long contentLength = Long.parseLong(header.getValue());
                if (contentLength > CommonsService.MAX_FILE_SIZE) {
                    throw new FileDecodingException(contentLength, "File too big: " + uri + " => " + contentLength);
                }
            }
        } catch (ProtocolException | NumberFormatException e) {
            LOGGER.warn(e.getMessage());
        }
        try (ClassicHttpResponse response = executeRequest(newHttpGet(uri), httpClient, context);
                InputStream in = response.getEntity().getContent()) {
            return readFile(uri, extension, localPath, readMetadata, in,
                response.getHeaders("Content-Disposition"), () -> response.getEntity().getContentLength());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ContentsAndMetadata<T> readFile(URI uri, String extension, Path localPath, boolean readMetadata,
            InputStream in, Header[] disposition, LongSupplier contentLength) throws IOException, FileDecodingException {
        boolean imageio = extension != null && IMAGEIO_EXTENSIONS.contains(extension);
        String filename = null;
        if (ArrayUtils.isNotEmpty(disposition)) {
            String value = disposition[0].getValue().replace("\"", "");
            if (value.startsWith("attachment;") && value.contains("filename=")) {
                filename = URLDecoder.decode(value.split("=")[1], "UTF-8").replace(";filename*", "");
            }
            if (!imageio) {
                extension = Utils.findExtension(value);
                imageio = extension != null && IMAGEIO_EXTENSIONS.contains(extension);
            }
        }
        String ext = extension;
        Function<URI, Optional<Path>> dl = x -> {
            try {
                long fileSize = contentLength.getAsLong();
                long usableSpace = new File(System.getProperty("java.io.tmpdir")).getUsableSpace();
                if (usableSpace < fileSize) {
                    LOGGER.error("Not enough usable disk space ({} bytes) to download {} ({} bytes). Aborting",
                            usableSpace, uri, fileSize);
                    return Optional.empty();
                }
                Path tempFile = Files.createTempFile("sm", "." + ext);
                try {
                    FileUtils.copyInputStreamToFile(in, tempFile.toFile());
                    return ofNullable(tempFile);
                } catch (IOException | RuntimeException e) {
                    Files.delete(tempFile);
                    throw e;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        if (imageio || extension == null || isBlank(extension)) {
            try {
                ContentsAndMetadata<BufferedImage> result = ImageUtils.readImage(in, readMetadata);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(result.contents(),
                        contentLength(contentLength, result::contentLength), filename,
                        isBlank(extension) ? result.extension() : extension,
                        result.numImagesOrPages(), null);
            } catch (IIOException e) {
                LOGGER.error("Image I/O error while reading {}: {}", uri, e.getMessage());
                GlitchTip.capture(e);
                return new ContentsAndMetadata<>(null, contentLength.getAsLong(), filename, extension, 1, e);
            }
        } else if ("www.youtube.com".equals(uri.getHost())) {
            return (ContentsAndMetadata<T>) readMetadataSafe(localPath, contentLength, filename, extension, uri,
                    x -> ofNullable(downloadYoutubeVideo(x.toString())));
        } else if (FileMetadata.VIDEO_EXTENSIONS.contains(extension)) {
            return (ContentsAndMetadata<T>) switch (extension) {
            case "avi", "mov", "mp4" -> readMetadataSafe(localPath, contentLength, filename, extension, uri, dl);
            case "ogv" -> readOgvVideo(in, contentLength.getAsLong(), filename, extension, uri);
            case "webm" -> readWebmVideo(localPath, contentLength, filename, extension, uri, dl);
            default -> throw new UnsupportedOperationException("Unsupported video format: " + extension);
            };
        } else if ("webp".equals(extension)) {
            ContentsAndMetadata<BufferedImage> result = ImageUtils.readWebp(uri, readMetadata);
            return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(result.contents(),
                    contentLength(contentLength, result::contentLength), filename, extension, 1, null);
        } else if ("pdf".equals(extension)) {
            try {
                PdfRandomAccessReadBuffer reader = new PdfRandomAccessReadBuffer(in);
                PDDocument pdf = org.apache.pdfbox.Loader.loadPDF(reader);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(pdf,
                        contentLength(contentLength, reader::size), filename, extension, pdf.getNumberOfPages(),
                        null);
            } catch (IOException e) {
                LOGGER.error("PDF I/O error while reading {}: {}", uri, e.getMessage());
                GlitchTip.capture(e);
                return new ContentsAndMetadata<>(null, contentLength.getAsLong(), filename, extension, 1, e);
            }
        } else if (POI_HSLF_EXTENSIONS.contains(extension) || POI_XSLF_EXTENSIONS.contains(extension)) {
            SlideShow<?, ?> ppt = readPowerpointFile(in, extension);
            return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(ppt, contentLength.getAsLong(), filename,
                    extension, ppt.getSlides().size(), null);
        } else if ("stl".equals(extension)) {
            // Assume readable
            byte[] bytes = in.readAllBytes();
            return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(new Object(),
                    contentLength(contentLength, () -> bytes.length), filename, extension, 1, null);
        } else if (FileMetadata.AUDIO_EXTENSIONS.contains(extension)) {
            return (ContentsAndMetadata<T>) readMetadataSafe(localPath, contentLength, filename, extension, uri, dl);
        } else {
            throw new FileDecodingException(contentLength.getAsLong(), "Unsupported format: " + extension);
        }
    }

    private static class PdfRandomAccessReadBuffer extends RandomAccessReadBuffer {

        public PdfRandomAccessReadBuffer(InputStream input) throws IOException {
            super(input);
        }

        public long size() {
            return size;
        }
    }

    private static long contentLength(LongSupplier first, LongSupplier second) {
        long result = first.getAsLong();
        return result > -1 ? result : second.getAsLong();
    }

    private static ContentsAndMetadata<OggFile> readOgvVideo(InputStream in, long contentLength, String filename,
            String extension, URI uri) throws FileDecodingException, IOException {
        try (OggFile ogg = new OggFile(in)) {
            if (ogg.getPacketReader().getNextPacket() == null) {
                throw new FileDecodingException(contentLength, "Failed to open OGV video from " + uri);
            }
            return new ContentsAndMetadata<>(ogg, contentLength, filename, extension, 1, null);
        }
    }

    private static <T> ContentsAndMetadata<T> readVideo(Path localPath, URI uri, LongSupplier contentLength,
            Function<URI, Optional<Path>> downloader, Function<Path, ContentsAndMetadata<T>> reader)
            throws FileDecodingException, IOException {
        try {
            if (localPath != null) {
                return reader.apply(localPath);
            } else {
                Path tempFile = downloader.apply(uri)
                        .orElseThrow(() -> new FileDecodingException(contentLength.getAsLong(),
                                "Failed to download video from " + uri));
                try {
                    return reader.apply(tempFile);
                } finally {
                    Files.delete(tempFile);
                }
            }
        } catch (UncheckedIOException e) {
            if (e.getCause().getCause() instanceof FileDecodingException fde) {
                throw fde;
            }
            throw e.getCause();
        }
    }

    private static ContentsAndMetadata<Metadata> readMetadata(Path path, LongSupplier contentLength, String filename, String extension)
            throws IOException, FileDecodingException {
        try {
            Metadata md = ImageMetadataReader.readMetadata(path.toFile());
            FileSystemDirectory fs = md.getFirstDirectoryOfType(FileSystemDirectory.class);
            if (fs == null) {
                throw new FileDecodingException(contentLength.getAsLong(), "Failed to read metadata from " + path + " / " + fs);
            }
            return new ContentsAndMetadata<>(md, contentLength(contentLength, () -> fs.getLongObject(TAG_FILE_SIZE)),
                    filename, extension, 1, null);
        } catch (ImageProcessingException e) {
            throw new FileDecodingException(contentLength.getAsLong(), e);
        }
    }

    private static ContentsAndMetadata<Metadata> readMetadataSafe(Path localPath, LongSupplier contentLength,
        String filename, String extension, URI uri, Function<URI, Optional<Path>> downloader) throws IOException, FileDecodingException {
            return readVideo(localPath, uri, contentLength, downloader, x -> {
                try {
                    try {
                        return readMetadata(x, contentLength, filename, extension);
                    } catch (FileDecodingException e) {
                        throw new IOException(e);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    private static ContentsAndMetadata<MediaDimensions> readWebmVideo(Path localPath, LongSupplier contentLength,
            String filename, String extension, URI uri, Function<URI, Optional<Path>> downloader)
            throws FileDecodingException, IOException {
        return readVideo(localPath, uri, contentLength, downloader, x -> {
            try {
                try {
                    return readWebmVideo(x, contentLength, filename, extension);
                } catch (FileDecodingException e) {
                    throw new IOException(e);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    // TODO: replace by metadata-extractor when https://github.com/drewnoakes/metadata-extractor/pull/679 is merged
    private static ContentsAndMetadata<MediaDimensions> readWebmVideo(Path path, LongSupplier contentLength,
            String filename, String extension) throws IOException, FileDecodingException {
        try {
            int width = 0;
            int height = 0;
            for (String line : Utils.execOutput(List.of("exiftool", path.toString()), 1, TimeUnit.MINUTES)
                    .split("\n")) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    switch (parts[0].trim()) {
                    case "Image Width":
                        width = Integer.parseInt(parts[1].trim());
                        break;
                    case "Image Height":
                        height = Integer.parseInt(parts[1].trim());
                        break;
                    default:
                        LOGGER.warn("Strange part: {}", parts[0]);
                    }
                }
            }
            if (width == 0 && height == 0) {
                throw new FileDecodingException(contentLength.getAsLong(), "Failed to open WEBM video from " + path);
            }
            return new ContentsAndMetadata<>(new MediaDimensions(width, height), contentLength.getAsLong(), filename,
                    extension, 1, null);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    public static Path downloadYoutubeVideo(String url) {
        try {
            String[] output = execOutput(
                    List.of("youtube-dl", "--no-progress", "--id", "--write-auto-sub", "--convert-subs", "srt", url),
                    30, TimeUnit.MINUTES).split("\n");
            Optional<Matcher> matcher = Arrays.stream(output).map(ALREADY_DL::matcher).filter(Matcher::matches)
                    .findFirst();
            if (matcher.isEmpty()) {
                matcher = Arrays.stream(output).map(MERGING_DL::matcher).filter(Matcher::matches).findFirst();
            }
            if (matcher.isEmpty() && Arrays.stream(output).anyMatch("[download] Download completed"::equals)) {
                matcher = Arrays.stream(output).map(DESTINATION::matcher).filter(Matcher::matches).findFirst();
            }
            if (matcher.isPresent()) {
                return Paths.get(matcher.get().group(1));
            }
            LOGGER.warn("Youtube video not downloaded?");
        } catch (IOException | ExecutionException e) {
            LOGGER.error("Error while downloading YouTube video: {}", e.getMessage());
            GlitchTip.capture(e);
        }
        return null;
    }

    public static SlideShow<?, ?> readPowerpointFile(InputStream in, String ext) throws IOException {
        if (POI_HSLF_EXTENSIONS.contains(ext)) {
            return new HSLFSlideShow(in);
        } else if (POI_XSLF_EXTENSIONS.contains(ext)) {
            return new XMLSlideShow(in);
        }
        throw new UnsupportedOperationException("Unsupported Powerpoint extension: " + ext);
    }

    public static Pair<Path, Long> convertPowerpointFileToPdf(Path path) throws IOException {
        try {
            PPTX2PNG.main(new String[] { "-format", "pdf", "-charset", "UTF-8", path.toString() });
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            Files.delete(path);
        }
        Path pdf = Paths.get(path.toString().replace(".pptx", ".pdf").replace(".pptm", ".pdf").replace(".ppt", ".pdf"));
        return Pair.of(pdf, Files.size(pdf));
    }
}
