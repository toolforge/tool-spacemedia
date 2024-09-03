package org.wikimedia.commons.donvip.spacemedia.service;

import static java.lang.Integer.parseInt;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.text.StringEscapeUtils.unescapeXml;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.computePerceptualHash;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.computeSha1;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.encode;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.similarityScore;
import static org.wikimedia.commons.donvip.spacemedia.utils.ImageUtils.readImageMetadata;
import static org.wikimedia.commons.donvip.spacemedia.utils.MediaUtils.readFile;

import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.util.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.ImageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.WikiPage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDescription;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.hashes.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.hashes.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.FileDecodingException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.utils.ContentsAndMetadata;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;

import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mp3.Mp3Directory;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.mp4.media.Mp4MediaDirectory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;
import com.drew.metadata.wav.WavDirectory;

@Lazy
@Service
public class MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private static final List<String> STRINGS_TO_REMOVE = Arrays.asList(" rel=\"noreferrer nofollow\"");

    private static final Map<String, String> STRINGS_TO_REPLACE = Map.of("&nbsp;", " ", "  ", " ", "â€™", "’", "ÔÇÖ",
            "’", "ÔÇ£", "«", "ÔÇØ", "»");

    // TODO update when https://github.com/haraldk/TwelveMonkeys/issues/884 is fixed
    private static final Set<String> IGNORED_IO_ERRORS = Set.of(
            "Unknown TIFF SampleFormat (expected 1, 2, 3 or 4): ",
            "destination width * height > Integer.MAX_VALUE: ",
            "Error: End-of-File, expected line at offset ");

    @Autowired
    private CommonsService commonsService;

    @Autowired
    private HashAssociationRepository hashRepository;

    @Autowired
    private FileMetadataRepository metadataRepository;

    @Autowired
    private ExifMetadataRepository exifRepository;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${perceptual.threshold.identicalid}")
    private double perceptualThresholdIdenticalId;

    @Value("${update.fullres.images}")
    private boolean updateFullResImages;

    @Value("${ignored.phash}")
    private Set<String> ignoredPhash;

    @Value("${ignored.sha1}")
    private Set<String> ignoredSha1;

    private Set<String> allowListAllowedTerms;
    private Set<String> blockListIgnoredTerms;
    private Set<String> copyrightsBlocklist;
    private Set<String> photographersBlocklist;

    @PostConstruct
    public void init() throws IOException {
        allowListAllowedTerms = CsvHelper.loadSet(getClass().getResource("/lists/allowlist.allowed.terms.csv"));
        blockListIgnoredTerms = CsvHelper.loadSet(getClass().getResource("/lists/blocklist.ignored.terms.csv"));
        copyrightsBlocklist = CsvHelper.loadSet(getClass().getResource("/lists/blocklist.ignored.copyrights.csv"));
        photographersBlocklist = CsvHelper.loadSet(getClass().getResource("/lists/blocklist.ignored.photographers.csv"));
    }

    public <M extends Media> MediaUpdateResult<M> updateMedia(MediaUpdateContext<M> ctx,
            Iterable<Pattern> patternsToRemove, Iterable<String> stringsToRemove,
            TriFunction<M, LocalDate, Integer, List<? extends Media>> similarCandidateMedia,
            boolean checkAllowlist, boolean checkBlocklist) throws IOException {
        return updateMedia(ctx, patternsToRemove, stringsToRemove, similarCandidateMedia, checkAllowlist, checkBlocklist, true);
    }

    public <M extends Media> MediaUpdateResult<M> updateMedia(MediaUpdateContext<M> ctx,
            Iterable<Pattern> patternsToRemove, Iterable<String> stringsToRemove,
            TriFunction<M, LocalDate, Integer, List<? extends Media>> similarCandidateMedia,
            boolean checkAllowlist, boolean checkBlocklist, boolean includeByPerceptualHash) throws IOException {
        boolean result = false;
        M media = ctx.media();
        GlitchTip.setTag("media", media.getIdUsedInOrg());
        GlitchTip.setTag("repo", media.getId().getRepoId());
        LOGGER.trace("updateMedia - cleanupDescription - {}", media);
        if (cleanupDescription(media, patternsToRemove, stringsToRemove)) {
            LOGGER.info("Description has been cleaned up for {}", media);
            result = true;
        }
        LOGGER.trace("updateMedia - updateReadableStateAndHashes - {}", media);
        MediaUpdateResult<M> ur = updateReadableStateAndHashes(ctx);
        if (ur.result()) {
            LOGGER.info("Readable state and/or hashes have been updated for {}", media);
            result = true;
        }
        LOGGER.trace("updateMedia - findCommonsFiles - {}", media);
        if (media.hasAssetsToUpload()
                && findCommonsFiles(media.getMetadata(), media.getSearchTermsInCommons(media.getMetadata()),
                        () -> similarCandidateMedia.apply(media, media.getPublicationDate(), 14),
                        includeByPerceptualHash)) {
            LOGGER.info("Commons files have been updated for {}", media);
            result = true;
        }
        LOGGER.trace("updateMedia - belongsToBlocklist - {}", media);
        if (checkAllowlist && !media.isIgnored() && !belongsToAllowlist(media)) {
            LOGGER.info("Allowlist check has been trigerred for {}", media);
            result = true;
        }
        if (checkBlocklist && !media.isIgnored() && belongsToBlocklist(media)) {
            LOGGER.info("Blocklist check has been trigerred for {}", media);
            result = true;
        }
        LOGGER.trace("updateMedia - done - {}", media);
        return new MediaUpdateResult<>(media, result, ur.exception());
    }

    protected String getTitleAndDescription(Media media) {
        StringBuilder sb = new StringBuilder();
        if (media.getTitle() != null) {
            sb.append(media.getTitle().toLowerCase(ENGLISH));
        }
        media.getDescriptions().stream().forEach(x -> sb.append(' ').append(x.toLowerCase(ENGLISH)));
        media.getAlbumNames().stream().forEach(x -> sb.append(' ').append(x.toLowerCase(ENGLISH)));
        return sb.toString().trim().replace("\r\n", " ").replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    protected String getMatchingTerms(Media media, Set<String> terms) {
        String titleAndDescription = getTitleAndDescription(media);
        return titleAndDescription.isEmpty() ? ""
                : blockListIgnoredTerms.parallelStream().filter(titleAndDescription::contains).sorted().collect(joining(","));
    }

    protected boolean belongsToAllowlist(Media media) {
        boolean result = !getMatchingTerms(media, allowListAllowedTerms).isEmpty();
        if (!result) {
            ignoreMedia(media, "Title or description does not contains any term from allow list");
        }
        return result;
    }

    protected boolean belongsToBlocklist(Media media) {
        String ignoredTerms = getMatchingTerms(media, blockListIgnoredTerms);
        if (!ignoredTerms.isEmpty()) {
            return ignoreMedia(media, "Title or description contains term(s) in block list: " + ignoredTerms);
        }
        boolean result = false;
        for (FileMetadata metadata : media.getMetadata()) {
            if (metadata != null && metadata.getExif() != null
                    && (metadata.getExif().getPhotographers().anyMatch(this::isPhotographerBlocklisted)
                            || metadata.getExif().getCopyrights().anyMatch(this::isCopyrightBlocklisted))) {
                result |= ignoreAndSaveMetadata(metadata,
                        "Probably non-free image (EXIF photographer/copyright blocklisted) : "
                                + metadata.getExif().getPhotographers().sorted().toList() + " / "
                                + metadata.getExif().getCopyrights().sorted().toList());
            }
        }
        return result;
    }

    public boolean isCopyrightBlocklisted(String copyright) {
        return copyrightsBlocklist.stream().anyMatch(copyright::contains);
    }

    public boolean isPhotographerBlocklisted(String photographer) {
        String normalizedPhotographer = photographer.toLowerCase(ENGLISH).replace(' ', '_');
        return photographersBlocklist.stream().anyMatch(normalizedPhotographer::startsWith);
    }

    public <M extends Media> MediaUpdateResult<M> updateReadableStateAndHashes(MediaUpdateContext<M> ctx) {
        boolean result = false;
        Exception exception = null;
        for (FileMetadata metadata : ctx.media().getMetadata()) {
            if (metadata.isIgnored() != Boolean.TRUE) {
                MediaUpdateResult<M> ur = updateReadableStateAndHashes(ctx, metadata);
                result |= ur.result();
                if (ur.exception() != null) {
                    exception = ur.exception();
                }
                if (ur.result()) {
                    LOGGER.info("Readable state and/or hashes have been updated for {}", metadata);
                }
            }
        }
        // T230284 - Processing full-res images can lead to OOM errors
        return new MediaUpdateResult<>(ctx.media(), result, exception);
    }

    public record MediaUpdateContext<M extends Media>(M media, Path localPath, UrlResolver<M> urlResolver,
            HttpClient httpClient, HttpClientContext context, boolean forceUpdateOfHashes, boolean ignoreExifMetadata) {
    }

    public record MediaUpdateResult<M extends Media>(M media, boolean result, boolean resetConsecutiveFailures,
            boolean incrementConsecutiveFailures, Exception exception) {

        public MediaUpdateResult(M media, boolean result, Exception exception) {
            this(media, result, false, false, exception);
        }
    }

    public <M extends Media> MediaUpdateResult<M> updateReadableStateAndHashes(MediaUpdateContext<M> ctx,
            FileMetadata metadata) {
        boolean result = false;
        Object contents = null;
        try {
            URL assetUrl = ctx.urlResolver.resolveDownloadUrl(ctx.media, metadata);
            result |= isBlank(metadata.getOriginalFileName())
                    && metadata.updateFilenameAndExtension(assetUrl.getPath());
            if (shouldReadFile(assetUrl, metadata, ctx.forceUpdateOfHashes)) {
                try {
                    ContentsAndMetadata<?> img = readFile(assetUrl, metadata.getFileExtension(), ctx.localPath, false,
                            true, ctx.httpClient, ctx.context);
                    contents = img.contents();
                    result |= updateReadableStateAndDims(metadata, img);
                    result |= updateFileSize(metadata, img);
                    result |= updateExtensionAndFilename(metadata, img);
                    if (img.ioException() != null) {
                        IOException e = img.ioException();
                        if (e.getMessage() != null
                                && IGNORED_IO_ERRORS.stream().anyMatch(x -> e.getMessage().startsWith(x))) {
                            LOGGER.error("I/O decoding error for {}: {}", assetUrl, e.getMessage());
                            GlitchTip.capture(e);
                            if (metadata.isReadable() == null || metadata.isAssumedReadable() == null) {
                                metadata.setReadable(Boolean.FALSE);
                                metadata.setAssumedReadable(Boolean.TRUE);
                                LOGGER.warn("Readable state has been FORCED to {} for {}", Boolean.TRUE, metadata);
                                result = true;
                            }
                        } else {
                            result = handleFileReadingError(metadata, e);
                        }
                    }
                } catch (FileDecodingException e) {
                    result |= updateFileSize(metadata, e.getContentLength(), null, null);
                    result |= handleFileReadingError(metadata, e);
                } catch (IOException | RestClientException e) {
                    result |= handleFileReadingError(metadata, e);
                }
            }
            if (contents instanceof BufferedImage bi && updatePerceptualHash(metadata, bi, ctx.forceUpdateOfHashes)) {
                LOGGER.info("Perceptual hash has been updated for {}", metadata);
                result = true;
            }
            contents = flushOrClose(contents);
            boolean isImage = metadata.isImage();
            boolean isReadableImage = isImage
                    && (Boolean.TRUE == metadata.isReadable() || Boolean.TRUE == metadata.isAssumedReadable());
            if ((!isImage || isReadableImage) && updateSha1(ctx, metadata)) {
                LOGGER.info("SHA1 hash has been updated for {}", metadata);
                result = true;
            }
            if (isReadableImage && !ctx.ignoreExifMetadata
                    && updateExifMetadata(metadata, ctx.httpClient, ctx.context)) {
                LOGGER.info("EXIF metadata has been updated for {}", metadata);
                result = true;
            }
        } catch (RestClientException e) {
            LOGGER.error("Error while computing hashes for {}: {}", ctx.media, e.getMessage());
            GlitchTip.capture(e);
            return new MediaUpdateResult<>(ctx.media, result, e);
        } catch (IOException e) {
            LOGGER.error("Error while computing hashes for {}", ctx.media, e);
            GlitchTip.capture(e);
            return new MediaUpdateResult<>(ctx.media, result, e);
        } finally {
            contents = flushOrClose(contents);
        }
        if (result) {
            saveMetadata(metadata);
        }
        return new MediaUpdateResult<>(ctx.media, result, null);
    }

    private static boolean handleFileReadingError(FileMetadata metadata, Exception e) {
        if (e.toString().contains("UnknownHostException")) {
            LOGGER.warn("Ignored file reading error of {} => {}", metadata.getAssetUri(), e.getMessage());
            return false;
        } else {
            boolean result = ignoreMetadata(metadata, "Unreadable file", e);
            metadata.setReadable(Boolean.FALSE);
            LOGGER.info("Readable state has been updated to {} for {}", Boolean.FALSE, metadata);
            return result;
        }
    }

    private static Object flushOrClose(Object contents) {
        if (contents instanceof BufferedImage bi) {
            bi.flush();
        } else if (contents instanceof Closeable c) {
            try {
                c.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close {}", contents, e);
                GlitchTip.capture(e);
            }
        }
        return null;
    }

    public boolean saveMetadata(FileMetadata metadata) {
        LOGGER.info("Saving {}", metadata);
        metadataRepository.save(metadata);
        return true;
    }

    static boolean updateReadableStateAndDims(FileMetadata metadata, ContentsAndMetadata<?> img) {
        boolean result = false;
        if (img.contents() != null && !Boolean.TRUE.equals(metadata.isReadable())) {
            metadata.setReadable(Boolean.TRUE);
            LOGGER.info("Readable state has been updated to {} for {}", Boolean.TRUE, metadata);
            result = true;
        }
        if ((!metadata.isAudio() && !metadata.hasValidDimensions())
                || ((metadata.isAudio() || metadata.isVideo()) && !metadata.hasValidDuration())) {
            if (img.contents() instanceof BufferedImage bi && bi.getWidth() > 0 && bi.getHeight() > 0) {
                metadata.setMediaDimensions(new MediaDimensions(bi.getWidth(), bi.getHeight()));
                LOGGER.info("Image dimensions have been updated for {}", metadata);
                result = true;
            } else if (img.contents() instanceof SlideShow<?, ?> ppt) {
                Dimension2D dim = Units.pointsToPixel(ppt.getPageSize());
                metadata.setMediaDimensions(new MediaDimensions((int) dim.getWidth(), (int) dim.getHeight()));
                LOGGER.info("PowerPoint dimensions have been updated for {}", metadata);
                result = true;
            } else if (img.contents() instanceof PDDocument pdf) {
                PDRectangle box = pdf.getPage(0).getMediaBox();
                metadata.setMediaDimensions(new MediaDimensions((int) box.getWidth(), (int) box.getHeight()));
                LOGGER.info("PDF dimensions have been updated for {}", metadata);
                result = true;
            } else if (img.contents() instanceof Metadata md) {
                if ((Object) md.getFirstDirectoryOfType(Mp4VideoDirectory.class) instanceof Mp4VideoDirectory mp4) {
                    result |= updateDimensions(metadata, mp4, md.getFirstDirectoryOfType(Mp4Directory.class),
                        Mp4VideoDirectory.TAG_WIDTH, Mp4VideoDirectory.TAG_HEIGHT, Mp4MediaDirectory.TAG_DURATION_SECONDS);
                } else if ((Object) md.getFirstDirectoryOfType(AviDirectory.class) instanceof AviDirectory avi) {
                    result |= updateDimensions(metadata, avi, avi,
                        AviDirectory.TAG_WIDTH, AviDirectory.TAG_HEIGHT, AviDirectory.TAG_DURATION);
                } else if ((Object) md.getFirstDirectoryOfType(QuickTimeVideoDirectory.class) instanceof QuickTimeVideoDirectory mov) {
                    result |= updateDimensions(metadata, mov, md.getFirstDirectoryOfType(QuickTimeDirectory.class),
                        QuickTimeVideoDirectory.TAG_WIDTH, QuickTimeVideoDirectory.TAG_HEIGHT, QuickTimeDirectory.TAG_DURATION_SECONDS);
                } else if ((Object) md.getFirstDirectoryOfType(Mp3Directory.class) instanceof Mp3Directory mp3) {
                    LOGGER.warn("MP3 duration not supported yet: https://github.com/drewnoakes/metadata-extractor/issues/492 => {}", mp3);
                } else if ((Object) md.getFirstDirectoryOfType(WavDirectory.class) instanceof WavDirectory wav) {
                    result |= updateDimensions(metadata, wav, wav, -1, -1, WavDirectory.TAG_DURATION);
                }
            } else if (img.contents() instanceof MediaDimensions dims) {
                metadata.setMediaDimensions(dims);
                LOGGER.info("Dimensions have been updated for {}", metadata);
                result = true;
            }
        }
        return result;
    }

    private static final boolean updateDimensions(FileMetadata metadata, Directory dirWh, Directory dirDur, int tagWidth, int tagHeight, int TagDuration) {
        Integer width = tagWidth != -1 ? dirWh.getInteger(tagWidth) : null;
        Integer height = tagHeight != -1 ? dirWh.getInteger(tagHeight) : null;
        Duration duration = null;
        Object obj = dirDur.getObject(TagDuration);
        if (obj instanceof Rational r) {
            // MP4 - ISO/IED 14496-12:2015 pg.23 - https://b.goeswhere.com/ISO_IEC_14496-12_2015.pdf
            // numerator = duration is an integer that declares length of the presentation (in the indicated timescale).
            //             This property is derived from the presentation’s tracks:
            //             the value of this field corresponds to the duration of the longest track in the presentation.
            //             If the duration cannot be determined then duration is set to all 1s.
            // denominator = timescale is an integer that specifies the time‐scale for the entire presentation;
            //               this is the number of time units that pass in one second.
            //               For example, a time coordinate system that measures time in sixtieths of a second has a time scale of 60.
            int timescale = (int) r.getDenominator();
            duration = timescale > 1 && timescale < 1000
                ? Duration.of(r.getNumerator() * 1000 / timescale, ChronoUnit.MILLIS)
                : switch(timescale) {
                case 1 -> Duration.of(r.getNumerator(), ChronoUnit.SECONDS);
                case 1_000 -> Duration.of(r.getNumerator(), ChronoUnit.MILLIS);
                case 1_000_000 -> Duration.of(r.getNumerator(), ChronoUnit.MICROS);
                case 1_000_000_000 -> Duration.of(r.getNumerator(), ChronoUnit.NANOS);
                default -> throw new UnsupportedOperationException(obj.toString());
            };
        } else if (obj instanceof String s) {
            String[] tab = s.split(":");
            duration = Duration.ofHours(parseInt(tab[0])).plusMinutes(parseInt(tab[1])).plusSeconds(parseInt(tab[2]));
        } else if (obj != null) {
            throw new UnsupportedOperationException(obj.toString());
        }
        if ((!metadata.hasValidDimensions() && width != null && width > 0 && height != null && height > 0)
            || (!metadata.hasValidDuration() && duration != null && duration.toNanos() > 0)) {
            metadata.setMediaDimensions(new MediaDimensions(width, height, duration));
            LOGGER.info("Audio/video dimensions have been updated for {}", metadata);
            return true;
        } else {
            LOGGER.warn("Invalid dimensions: {} => {} / {}", metadata, dirWh, dirDur);
            return false;
        }
    }

    private static boolean updateFileSize(FileMetadata metadata, ContentsAndMetadata<?> img) {
        return updateFileSize(metadata, img.contentLength(), img.contents(), img.extension());
    }

    private static boolean updateFileSize(FileMetadata metadata, long contentLength, Object contents, String ext) {
        if (!metadata.hasSize()) {
            if (contentLength > 0) {
                metadata.setSize(contentLength);
                LOGGER.info("Size has been updated from contentLength for {}", metadata);
                return true;
            } else if (contents instanceof BufferedImage bi && isNotBlank(ext)) {
                try {
                    Path tempFile = Files.createTempFile("sm", ext);
                    try {
                        if (ImageIO.write(bi, ext, tempFile.toFile())) {
                            metadata.setSize(Files.size(tempFile));
                            LOGGER.info("Size has been updated from file size for {}", metadata);
                            return true;
                        } else {
                            LOGGER.warn("Failed to write {} to {}", metadata, tempFile);
                        }
                    } finally {
                        Files.delete(tempFile);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to compute file size for {}", metadata, e);
                    GlitchTip.capture(e);
                }
            } else {
                LOGGER.warn("Unable to update file size of {}", metadata);
            }
        }
        return false;
    }

    private static boolean updateExtensionAndFilename(FileMetadata metadata, ContentsAndMetadata<?> img) {
        boolean result = false;
        if (isNotBlank(img.extension()) && isBlank(metadata.getExtension())) {
            metadata.setExtension(img.extension());
            LOGGER.info("Extension has been updated for {}", metadata);
            result = true;
        }
        if (isNotBlank(img.filename()) && isBlank(metadata.getOriginalFileName())) {
            metadata.setOriginalFileName(img.filename());
            LOGGER.info("Filename has been updated for {}", metadata);
            result = true;
        }
        return result;
    }

    private static boolean shouldReadFile(URL assetUrl, FileMetadata metadata, boolean forceUpdateOfHashes) {
        return assetUrl != null && (forceUpdateOfHashes || metadata.shouldRead());
    }

    public boolean ignoreMedia(Media media, String reason) {
        return ignoreMedia(media, reason, null);
    }

    public boolean ignoreMedia(Media media, String reason, Exception e) {
        media.getMetadataStream().forEach(fm -> ignoreAndSaveMetadata(fm, reason, e));
        return true;
    }

    public boolean ignoreAndSaveMetadata(FileMetadata fm, String reason) {
        return ignoreAndSaveMetadata(fm, reason, null);
    }

    public boolean ignoreAndSaveMetadata(FileMetadata fm, String reason, Exception e) {
        ignoreMetadata(fm, reason, e);
        return saveMetadata(fm);
    }

    public static boolean ignoreMetadata(FileMetadata fm, String reason) {
        return ignoreMetadata(fm, reason, null);
    }

    public static boolean ignoreMetadata(FileMetadata fm, String reason, Exception e) {
        if (e != null) {
            LOGGER.warn("Ignored {} for reason {}: {}", fm, reason, e.toString());
        } else {
            LOGGER.warn("Ignored {} for reason {}", fm, reason);
        }
        fm.setIgnored(Boolean.TRUE);
        fm.setIgnoredReason(reason + (e != null ? ": " + e.getMessage() : ""));
        return true;
    }

    public static boolean cleanupDescription(Media media, Iterable<Pattern> patternsToRemove,
            Iterable<String> stringsToRemove) {
        boolean result = false;
        for (MediaDescription md : media.getDescriptionObjects()) {
            String description = md.getDescription();
            if (isNotBlank(description)) {
                description = unescapeXml(description);
                if (patternsToRemove != null) {
                    for (Pattern toRemove : patternsToRemove) {
                        description = toRemove.matcher(description).replaceAll("").trim();
                    }
                }
                if (stringsToRemove != null) {
                    for (String toRemove : stringsToRemove) {
                        if (description.contains(toRemove)) {
                            description = description.replace(toRemove, "").trim();
                        }
                    }
                }
                for (String toRemove : STRINGS_TO_REMOVE) {
                    if (description.contains(toRemove)) {
                        description = description.replace(toRemove, "");
                    }
                }
                for (Entry<String, String> toReplace : STRINGS_TO_REPLACE.entrySet()) {
                    while (description.contains(toReplace.getKey())) {
                        description = description.replace(toReplace.getKey(), toReplace.getValue());
                    }
                }
                result = !description.equals(md.getDescription());
                if (result) {
                    md.setDescription(description);
                }
            }
        }
        return result;
    }

    public boolean updateExifMetadata(FileMetadata metadata, HttpClient httpClient, HttpClientContext context)
            throws IOException {
        if (metadata.getExif() == null) {
            try {
                metadata.setExif(exifRepository
                        .save(ExifMetadata.of(readImageMetadata(metadata.getAssetUri(), httpClient, context))));
                return true;
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to update EXIF metadata for {}: {}", metadata, e.getMessage());
                GlitchTip.capture(e);
            }
        }
        return false;
    }

    /**
     * Computes the media SHA-1.
     *
     * @param ctx media update context
     * @param metadata media object metadata
     * @return {@code true} if media has been updated with computed SHA-1 and must be persisted
     * @throws IOException        in case of I/O error
     */
    public <M extends Media> boolean updateSha1(MediaUpdateContext<M> ctx, FileMetadata metadata)
            throws IOException {
        if ((!metadata.hasSha1() || ctx.forceUpdateOfHashes)
                && (metadata.getAssetUrl() != null || ctx.localPath != null)) {
            metadata.setSha1(ctx.localPath != null ? computeSha1(ctx.localPath)
                    : computeSha1(ctx.urlResolver.resolveDownloadUrl(ctx.media, metadata), ctx.httpClient,
                            ctx.context));
            updateHashes(metadata.getSha1(), metadata.getPhash(), metadata.getMime());
            return true;
        }
        return false;
    }

    private void updateHashes(String sha1, String phash, String mime) {
        if (sha1 != null) {
            String sha1base36 = CommonsService.base36Sha1(sha1);
            if (!hashRepository.existsById(sha1base36)) {
                hashRepository.save(new HashAssociation(sha1base36, phash, mime));
            }
        }
    }

    /**
     * Computes the perceptual hash of an image, if required.
     *
     * @param metadata  image media metadata
     * @param image     {@code BufferedImage} of asset, can be null if not computed
     * @param forceUpdate {@code true} to force update of an existing hash
     * @return {@code true} if media has been updated with computed perceptual hash
     *         and must be persisted
     */
    public boolean updatePerceptualHash(FileMetadata metadata, BufferedImage image, boolean forceUpdate) {
        if (image != null && (!metadata.hasPhash() || forceUpdate)) {
            try {
                metadata.setPhash(encode(computePerceptualHash(image)));
            } catch (RuntimeException e) {
                LOGGER.error("Failed to update perceptual hash for {}", metadata, e);
                GlitchTip.capture(e);
            }
            updateHashes(metadata.getSha1(), metadata.getPhash(), metadata.getMime());
            return true;
        }
        return false;
    }

    public boolean findCommonsFiles(Collection<FileMetadata> metadata, Collection<String> searchTermsInCommons,
            Supplier<List<? extends Media>> similarCandidateMedia, boolean includeByPerceptualHash) throws IOException {
        return findCommonsFilesWithSha1(metadata) || (includeByPerceptualHash
                && (findCommonsFilesWithPhash(metadata, true)
                        || findCommonsFilesWithTextAndPhash(metadata, searchTermsInCommons)
                        || findCommonsFilesWithPublicationDateAndPhash(metadata, similarCandidateMedia)));
    }

    /**
     * Looks for Wikimedia Commons files matching the metadata SHA-1, if required.
     *
     * @param metadatas list of file metadata objects
     * @return {@code true} if at least one metadata has been updated with list of
     *         Wikimedia Commons files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithSha1(Collection<FileMetadata> metadatas) throws IOException {
        boolean result = false;
        for (FileMetadata metadata : metadatas) {
            result |= findCommonsFilesWithSha1(metadata);
        }
        return result;
    }

    private boolean findCommonsFilesWithSha1(FileMetadata metadata) throws IOException {
        if (shouldSearchBySha1(metadata)) {
            Set<String> files = commonsService.findFilesWithSha1(metadata.getSha1());
            if (!files.isEmpty()) {
                return saveNewMetadataCommonsFileNames(metadata, files);
            }
        }
        return false;
    }

    /**
     * Looks for Wikimedia Commons files matching exactly the metadata perceptual
     * hash, if required.
     *
     * @param metadatas       list of file metadata objects
     * @param excludeSelfSha1 whether to exclude metadata's own sha1 from search
     * @return {@code true} if at least one metadata has been updated with list of
     *         Wikimedia Commons files and must be persisted
     * @throws IOException in case of I/O error
     */
    public boolean findCommonsFilesWithPhash(Collection<FileMetadata> metadatas, boolean excludeSelfSha1)
            throws IOException {
        boolean result = false;
        for (FileMetadata metadata : metadatas) {
            if (findCommonsFilesWithPhash(metadata, excludeSelfSha1)) {
                result = true;
            }
        }
        return result;
    }

    private boolean findCommonsFilesWithPhash(FileMetadata metadata, boolean excludeSelfSha1) throws IOException {
        if (shouldSearchByPhash(metadata)) {
            List<String> sha1s = hashRepository.findSha1ByPhashAndMime(metadata.getPhash(), metadata.getMime());
            if (excludeSelfSha1) {
                sha1s.remove(CommonsService.base36Sha1(metadata.getSha1()));
            }
            if (!sha1s.isEmpty()) {
                Set<String> files = commonsService.findFilesWithSha1(sha1s);
                if (!files.isEmpty()) {
                    return saveNewMetadataCommonsFileNames(metadata, files);
                }
            }
        }
        return false;
    }

    public boolean findCommonsFilesWithTextAndPhash(Collection<FileMetadata> metadatas, Collection<String> texts)
            throws IOException {
        boolean result = false;
        for (String text : texts) {
            if (StringUtils.isNotBlank(text)) {
                try {
                    Collection<WikiPage> images = commonsService.searchImages(text.strip());
                    if (!images.isEmpty()) {
                        for (FileMetadata metadata : metadatas) {
                            if (findCommonsFilesWithSearchTermAndPhash(images, metadata)) {
                                result = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to search images: {}", e.getMessage());
                    GlitchTip.capture(e);
                }
            }
        }
        return result;
    }

    public boolean findCommonsFilesWithPublicationDateAndPhash(Collection<FileMetadata> metadatas,
            Supplier<List<? extends Media>> similarCandidateMedia) {
        boolean result = false;
        Collection<FileMetadata> metadataToSearch = metadatas.stream().filter(this::shouldSearchByPhash).toList();
        if (!metadataToSearch.isEmpty()) {
            List<FileMetadata> similarCandidateFiles = similarCandidateMedia.get().stream()
                    .flatMap(Media::getMetadataStream).filter(fm -> !fm.getCommonsFileNames().isEmpty()).distinct()
                    .toList();
            for (FileMetadata metadata : metadataToSearch) {
                if (metadata.hasSize()) {
                    result |= findCommonsFilesWithPublicationDateAndPhash(metadata, similarCandidateFiles);
                } else {
                    LOGGER.warn("File without size, skipping: {}", metadata);
                }
            }
        }
        return result;
    }

    private boolean findCommonsFilesWithPublicationDateAndPhash(FileMetadata metadata,
            List<FileMetadata> similarCandidateFiles) {
        Set<String> filenames = new HashSet<>();
        for (FileMetadata similarCandidateFile : similarCandidateFiles) {
            if (!similarCandidateFile.hasSize() || !similarCandidateFile.hasValidDimensions()
                    || isBlank(similarCandidateFile.getMime())
                    || similarCandidateFile.getCommonsFileNames().isEmpty()) {
                LOGGER.warn("Invalid candidate file, skipping: {}", similarCandidateFile);
                continue;
            }
            if (StringUtils.equals(metadata.getMime(), similarCandidateFile.getMime())
                    && (metadata.getSize() <= similarCandidateFile.getSize() || areLargerOrEqualDimensions(
                            metadata.getMediaDimensions(), similarCandidateFile.getMediaDimensions()))
                    && phashMatches(metadata, similarCandidateFile.getCommonsFileNames().iterator().next(),
                            similarCandidateFile.getPhash())) {
                filenames.addAll(similarCandidateFile.getCommonsFileNames());
            }
        }
        return !filenames.isEmpty() && saveNewMetadataCommonsFileNames(metadata, filenames);
    }

    public List<String> findSmallerCommonsFilesWithSearchTermAndPhash(Media media, FileMetadata metadata) throws IOException {
        List<String> result = new ArrayList<>();
        for (String searchTerm : media.getSearchTermsInCommons(List.of(metadata))) {
            try {
                result.addAll(findCommonsFilesWithSearchTermAndPhashFiltered(commonsService.searchImages(searchTerm),
                        metadata, MediaService::filterBySameMimeAndSmallerSize));
            } catch (IOException e) {
                LOGGER.error("Failed to search images: {}", e.getMessage());
                GlitchTip.capture(e);
            }
        }
        return result;
    }

    private boolean findCommonsFilesWithSearchTermAndPhash(Collection<WikiPage> images, FileMetadata metadata) {
        List<String> filenames = findCommonsFilesWithSearchTermAndPhashFiltered(images, metadata,
                MediaService::filterBySameMimeAndLargerOrEqualSizeOrLargerOrEqualDimensions);
        return !filenames.isEmpty() && saveNewMetadataCommonsFileNames(metadata, new HashSet<>(filenames));
    }

    private List<String> findCommonsFilesWithSearchTermAndPhashFiltered(Collection<WikiPage> images, FileMetadata metadata,
            BiPredicate<FileMetadata, ImageInfo> filter) {
        List<String> filenames = new ArrayList<>();
        if (shouldSearchByPhash(metadata)) {
            List<WikiPage> list = images.stream().filter(i -> filter.test(metadata, i.getImageInfo()[0])).toList();
            LOGGER.debug("Searching match by id/phash for {} => filtered to {}", images, list);
            for (WikiPage image : list) {
                String filename = image.getTitle().replace("File:", "").replace(' ', '_');
                String sha1base36 = CommonsService.base36Sha1(image.getImageInfo()[0].getSha1());
                Optional<HashAssociation> hash = hashRepository.findById(sha1base36);
                if (hash.isEmpty()) {
                    hash = Optional.ofNullable(commonsService.computeAndSaveHash(sha1base36, filename,
                            FileMetadata.getMime(filename.substring(filename.lastIndexOf('.') + 1))));
                }
                if (phashMatches(metadata, filename,
                        hash.orElseThrow(() -> new IllegalStateException("No hash for " + sha1base36)).getPhash())) {
                    filenames.add(filename);
                }
            }
        }
        return filenames;
    }

    private boolean phashMatches(FileMetadata metadata, String filename, String phash) {
        if (phash != null) {
            double score = similarityScore(metadata.getPhash(), phash);
            if (score <= perceptualThresholdIdenticalId) {
                LOGGER.info("Found match ({}) between {} and {} / {}", score, metadata, filename, phash);
                return true;
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No match between {} and {} / {} -> {}", metadata, filename, phash, score);
            }
        }
        return false;
    }

    private boolean shouldSearchByPhash(FileMetadata metadata) {
        return metadata.hasPhash() && !ignoredPhash.contains(metadata.getPhash())
                && isEmpty(metadata.getCommonsFileNames());
    }

    private boolean shouldSearchBySha1(FileMetadata metadata) {
        return metadata.hasSha1() && !ignoredSha1.contains(metadata.getSha1())
                && isEmpty(metadata.getCommonsFileNames());
    }

    private static boolean filterBySameMimeAndLargerOrEqualSizeOrLargerOrEqualDimensions(FileMetadata metadata,
            ImageInfo imageInfo) {
        return StringUtils.equals(metadata.getMime(), imageInfo.getMime()) && metadata.hasSize()
                && (metadata.getSize() <= imageInfo.getSize()
                        || areLargerOrEqualDimensions(metadata.getMediaDimensions(), imageInfo));
    }

    private static boolean areLargerOrEqualDimensions(MediaDimensions dims, ImageInfo imageInfo) {
        return areLargerOrEqualDimensions(dims, imageInfo.getWidth(), imageInfo.getHeight());
    }

    private static boolean areLargerOrEqualDimensions(MediaDimensions dims, MediaDimensions other) {
        return areLargerOrEqualDimensions(dims, other.getWidth(), other.getHeight());
    }

    private static boolean areLargerOrEqualDimensions(MediaDimensions dims, int width, int height) {
        return dims != null && dims.getWidth() <= width && dims.getHeight() <= height;
    }

    private static boolean filterBySameMimeAndSmallerSize(FileMetadata metadata, ImageInfo imageInfo) {
        return StringUtils.equals(metadata.getMime(), imageInfo.getMime()) && metadata.hasSize()
                && metadata.getSize() > imageInfo.getSize();
    }

    public boolean saveNewMetadataCommonsFileNames(FileMetadata metadata, Set<String> commonsFileNames) {
        LOGGER.info("Saving new commons filenames {} for {}", commonsFileNames, metadata);
        metadata.setCommonsFileNames(commonsFileNames);
        return saveMetadata(metadata);
    }

    public <T> void useMapping(Set<String> result, String key, Set<T> items,
            Map<String, Map<String, String>> mappings, Function<T, String> keyFunction) {
        if (CollectionUtils.isNotEmpty(items)) {
            Map<String, String> mapping = mappings.get(key);
            if (MapUtils.isNotEmpty(mapping)) {
                for (T item : items) {
                    String cats = mapping.get(keyFunction.apply(item));
                    if (isNotBlank(cats)) {
                        Arrays.stream(cats.split(";")).map(String::trim).forEach(result::add);
                    }
                }
            }
        }
    }
}
