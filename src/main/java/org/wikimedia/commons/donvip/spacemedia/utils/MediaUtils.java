package org.wikimedia.commons.donvip.spacemedia.utils;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.execOutput;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.util.PPTX2PNG;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.exception.FileDecodingException;

import nu.pattern.OpenCV;

public class MediaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUtils.class);

    private static final Set<String> IMAGEIO_EXTENSIONS = Set.of("bmp", "gif", "jpe", "jpg", "jpeg", "png", "svg",
            "tif", "tiff");

    private static final Set<String> POI_HSLF_EXTENSIONS = Set.of("ppt");
    private static final Set<String> POI_XSLF_EXTENSIONS = Set.of("pptm", "pptx");

    private static final Pattern MERGING_DL = Pattern
            .compile("\\[ffmpeg\\] Merging formats into \"(\\S{11}\\.\\S{3,4})\"");
    private static final Pattern ALREADY_DL = Pattern
            .compile("\\[download\\] (\\S{11}\\.\\S{3,4}) has already been downloaded and merged");

    static {
        OpenCV.loadLocally();
    }

    private MediaUtils() {
        // Hide default constructor
    }

    public static <T> ContentsAndMetadata<T> readFile(URL url, String extension, Path localPath, boolean readMetadata,
            boolean log) throws IOException, FileDecodingException {
        return readFile(Utils.urlToUriUnchecked(url), extension, localPath, readMetadata, log);
    }

    @SuppressWarnings("unchecked")
    public static <T> ContentsAndMetadata<T> readFile(URI uri, String extension, Path localPath, boolean readMetadata,
            boolean log) throws IOException, FileDecodingException {
        if (log) {
            LOGGER.info("Reading file {}", uri);
        }
        if (isBlank(extension)) {
            extension = Utils.findExtension(uri.toString());
        }
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            boolean imageio = extension != null && IMAGEIO_EXTENSIONS.contains(extension);
            String filename = null;
            if (!imageio) {
                Header[] disposition = response.getHeaders("Content-Disposition");
                if (ArrayUtils.isNotEmpty(disposition)) {
                    String value = disposition[0].getValue().replace("\"", "");
                    if (value.startsWith("attachment;filename=")) {
                        filename = URLDecoder.decode(value.split("=")[1], "UTF-8").replace(";filename*", "");
                    }
                    extension = Utils.findExtension(value);
                    imageio = extension != null && IMAGEIO_EXTENSIONS.contains(extension);
                }
            }
            long contentLength = response.getEntity().getContentLength();
            if (imageio || isBlank(extension)) {
                ContentsAndMetadata<BufferedImage> result = ImageUtils.readImage(in, readMetadata);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(result.contents(), contentLength, filename,
                        isBlank(extension) ? result.extension() : extension, result.numImagesOrPages());
            } else if ("www.youtube.com".equals(uri.getHost())) {
                if (localPath != null) {
                    return (ContentsAndMetadata<T>) readVideo(localPath, contentLength, filename, extension);
                } else {
                    Path tempFile = ofNullable(downloadYoutubeVideo(uri.toString()))
                            .orElseThrow(() -> new FileDecodingException("Failed to download video from " + uri));
                    try {
                        return (ContentsAndMetadata<T>) readVideo(tempFile, contentLength, filename, extension);
                    } finally {
                        Files.delete(tempFile);
                    }
                }
            } else if (FileMetadata.VIDEO_EXTENSIONS.contains(extension)) {
                Path tempFile = Files.createTempFile("sm", "." + extension);
                try {
                    FileUtils.copyInputStreamToFile(in, tempFile.toFile());
                    return (ContentsAndMetadata<T>) readVideo(tempFile, contentLength, filename, extension);
                } finally {
                    Files.delete(tempFile);
                }
            } else if ("webp".equals(extension)) {
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(
                        ImageUtils.readWebp(uri, readMetadata).contents(), contentLength, filename, extension, 1);
            } else if ("pdf".equals(extension)) {
                PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(in));
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(pdf, contentLength, filename, extension,
                        pdf.getNumberOfPages());
            } else if (POI_HSLF_EXTENSIONS.contains(extension) || POI_XSLF_EXTENSIONS.contains(extension)) {
                SlideShow<?, ?> ppt = readPowerpointFile(in, extension);
                return (ContentsAndMetadata<T>) new ContentsAndMetadata<>(ppt, contentLength, filename, extension,
                        ppt.getSlides().size());
            } else {
                throw new FileDecodingException(
                        "Unsupported format: " + extension + " / headers:" + Arrays.stream(response.getAllHeaders())
                                .map(h -> h.getName() + ": " + h.getValue()).sorted().toList());
            }
        }
    }

    private static ContentsAndMetadata<VideoCapture> readVideo(Path path, long contentLength, String filename,
            String extension) throws FileDecodingException {
        VideoCapture vc = new VideoCapture();
        if (!vc.open(path.toString())) {
            throw new FileDecodingException("Failed to open video from " + path);
        }
        return new ContentsAndMetadata<>(vc, contentLength, filename, extension,
                (int) vc.get(Videoio.CAP_PROP_FRAME_COUNT));
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
            if (matcher.isPresent()) {
                return Paths.get(matcher.get().group(1));
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.error("Error while downloading YouTube video: {}", e.getMessage());
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
        Path pdf = Paths.get(path + ".pdf");
        try {
            PPTX2PNG.main(new String[] { "-format", "pdf", "-charset", "UTF-8", "-outfile", pdf.toString(),
                    path.toString() });
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            Files.delete(path);
        }
        return Pair.of(pdf, Files.size(pdf));
    }
}
