package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.Optional.ofNullable;
import static javax.persistence.GenerationType.SEQUENCE;

import java.net.URL;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.lang3.StringUtils;
import org.wikimedia.commons.donvip.spacemedia.utils.StringArrayAsStringDeserializer;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileTypeDirectory;
import com.drew.metadata.iptc.IptcDescriptor;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.photoshop.PhotoshopDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExifMetadata {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = SEQUENCE, generator = "exif_sequence")
    private long id;

    @JsonProperty("File:FileType")
    private String fileFileType;

    @JsonProperty("File:FileTypeExtension")
    private String fileFileTypeExtension;

    @JsonProperty("File:ImageHeight")
    private int fileImageHeight;

    @JsonProperty("File:ImageWidth")
    private int fileImageWidth;

    @JsonProperty("File:MIMEType")
    private String fileMimeType;

    @Lob
    @JsonProperty("EXIF:Artist")
    @Column(columnDefinition = "TEXT")
    private String exifArtist;

    @JsonProperty("EXIF:Copyright")
    private String exifCopyright;

    @JsonProperty("EXIF:ExifImageHeight")
    private int exifExifImageHeight;

    @JsonProperty("EXIF:ExifImageWidth")
    private int exifExifImageWidth;

    @Lob
    @JsonProperty("EXIF:ImageDescription")
    @Column(columnDefinition = "TEXT")
    private String exifImageDescription;

    @JsonProperty("EXIF:ImageHeight")
    private int exifImageHeight;

    @JsonProperty("EXIF:ImageWidth")
    private int exifImageWidth;

    @JsonProperty("EXIF:LensSerialNumber")
    private String exifLensSerialNumber;

    @JsonProperty("EXIF:SerialNumber")
    private String exifSerialNumber;

    @JsonDeserialize(using = StringArrayAsStringDeserializer.class)
    @JsonProperty("IPTC:By-line")
    private String iptcByLine;

    @Lob
    @JsonProperty("IPTC:Caption-Abstract")
    @Column(columnDefinition = "TEXT")
    private String iptcCaptionAbstract;

    @JsonProperty("IPTC:CopyrightNotice")
    private String iptcCopyrightNotice;

    @Lob
    @JsonProperty("IPTC:ObjectName")
    @Column(columnDefinition = "TEXT")
    private String iptcObjectName;

    @JsonProperty("IPTC:Writer-Editor")
    private String iptcWriterEditor;

    @JsonProperty("Photoshop:URL")
    private URL photoshopUrl;

    @Lob
    @JsonProperty("XMP:CaptionWriter")
    @Column(columnDefinition = "TEXT")
    private String xmpCaptionWriter;

    @Lob
    @JsonProperty("XMP:Description")
    @Column(columnDefinition = "TEXT")
    private String xmpDescription;

    @JsonProperty("XMP:Format")
    private String xmpFormat;

    @JsonProperty("XMP:LensSerialNumber")
    private String xmpLensSerialNumber;

    @JsonProperty("XMP:OriginalDocumentID")
    private String xmpOriginalDocumentID;

    @JsonProperty("XMP:Rights")
    private String xmpRights;

    @JsonProperty("XMP:SerialNumber")
    private String xmpSerialNumber;

    @Lob
    @JsonProperty("XMP:Title")
    @Column(columnDefinition = "TEXT")
    private String xmpTitle;

    @JsonProperty("XMP:WebStatement")
    private String xmpWebStatement;

    @JsonProperty("Composite:Megapixels")
    private Float compositeMegaPixels;

    @JsonProperty("AVAIL:NASAID")
    private String availNasaId;

    @JsonProperty("AVAIL:Center")
    private String availCenter;

    @Lob
    @JsonProperty("AVAIL:Description")
    @Column(columnDefinition = "TEXT")
    private String availDescription;

    @Lob
    @JsonProperty("AVAIL:Description508")
    @Column(columnDefinition = "TEXT")
    private String availDescription508;

    @JsonProperty("AVAIL:Location")
    private String availLocation;

    @Lob
    @JsonProperty("AVAIL:Photographer")
    @Column(columnDefinition = "TEXT")
    private String availPhotographer;

    @JsonProperty("AVAIL:SecondaryCreator")
    private String availSecondaryCreator;

    @Lob
    @JsonProperty("AVAIL:Title")
    @Column(columnDefinition = "TEXT")
    private String availTitle;

    @JsonIgnore
    public long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(long id) {
        this.id = id;
    }

    public String getFileFileType() {
        return fileFileType;
    }

    public void setFileFileType(String fileFileType) {
        this.fileFileType = fileFileType;
    }

    public String getFileFileTypeExtension() {
        return fileFileTypeExtension;
    }

    public void setFileFileTypeExtension(String fileFileTypeExtension) {
        this.fileFileTypeExtension = fileFileTypeExtension;
    }

    public int getFileImageHeight() {
        return fileImageHeight;
    }

    public void setFileImageHeight(int fileImageHeight) {
        this.fileImageHeight = fileImageHeight;
    }

    public int getFileImageWidth() {
        return fileImageWidth;
    }

    public void setFileImageWidth(int fileImageWidth) {
        this.fileImageWidth = fileImageWidth;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public void setFileMimeType(String fileMimeType) {
        this.fileMimeType = fileMimeType;
    }

    public String getExifArtist() {
        return exifArtist;
    }

    public void setExifArtist(String exifArtist) {
        this.exifArtist = exifArtist;
    }

    public String getExifCopyright() {
        return exifCopyright;
    }

    public void setExifCopyright(String exifCopyright) {
        this.exifCopyright = exifCopyright;
    }

    public int getExifExifImageHeight() {
        return exifExifImageHeight;
    }

    public void setExifExifImageHeight(int exifExifImageHeight) {
        this.exifExifImageHeight = exifExifImageHeight;
    }

    public int getExifExifImageWidth() {
        return exifExifImageWidth;
    }

    public void setExifExifImageWidth(int exifExifImageWidth) {
        this.exifExifImageWidth = exifExifImageWidth;
    }

    public String getExifImageDescription() {
        return exifImageDescription;
    }

    public void setExifImageDescription(String exifImageDescription) {
        this.exifImageDescription = exifImageDescription;
    }

    public int getExifImageHeight() {
        return exifImageHeight;
    }

    public void setExifImageHeight(int exifImageHeight) {
        this.exifImageHeight = exifImageHeight;
    }

    public int getExifImageWidth() {
        return exifImageWidth;
    }

    public void setExifImageWidth(int exifImageWidth) {
        this.exifImageWidth = exifImageWidth;
    }

    public String getExifLensSerialNumber() {
        return exifLensSerialNumber;
    }

    public void setExifLensSerialNumber(String exifLensSerialNumber) {
        this.exifLensSerialNumber = exifLensSerialNumber;
    }

    public String getExifSerialNumber() {
        return exifSerialNumber;
    }

    public void setExifSerialNumber(String exifSerialNumber) {
        this.exifSerialNumber = exifSerialNumber;
    }

    public String getIptcByLine() {
        return iptcByLine;
    }

    public void setIptcByLine(String iptcByLine) {
        this.iptcByLine = iptcByLine;
    }

    public String getIptcCaptionAbstract() {
        return iptcCaptionAbstract;
    }

    public void setIptcCaptionAbstract(String iptcCaptionAbstract) {
        this.iptcCaptionAbstract = iptcCaptionAbstract;
    }

    public String getIptcCopyrightNotice() {
        return iptcCopyrightNotice;
    }

    public void setIptcCopyrightNotice(String iptcCopyrightNotice) {
        this.iptcCopyrightNotice = iptcCopyrightNotice;
    }

    public String getIptcObjectName() {
        return iptcObjectName;
    }

    public void setIptcObjectName(String iptcObjectName) {
        this.iptcObjectName = iptcObjectName;
    }

    public String getIptcWriterEditor() {
        return iptcWriterEditor;
    }

    public void setIptcWriterEditor(String iptcWriterEditor) {
        this.iptcWriterEditor = iptcWriterEditor;
    }

    public URL getPhotoshopUrl() {
        return photoshopUrl;
    }

    public void setPhotoshopUrl(URL photoshopUrl) {
        this.photoshopUrl = photoshopUrl;
    }

    public String getXmpCaptionWriter() {
        return xmpCaptionWriter;
    }

    public void setXmpCaptionWriter(String xmpCaptionWriter) {
        this.xmpCaptionWriter = xmpCaptionWriter;
    }

    public String getXmpDescription() {
        return xmpDescription;
    }

    public void setXmpDescription(String xmpDescription) {
        this.xmpDescription = xmpDescription;
    }

    public String getXmpFormat() {
        return xmpFormat;
    }

    public void setXmpFormat(String xmpFormat) {
        this.xmpFormat = xmpFormat;
    }

    public String getXmpLensSerialNumber() {
        return xmpLensSerialNumber;
    }

    public void setXmpLensSerialNumber(String xmpLensSerialNumber) {
        this.xmpLensSerialNumber = xmpLensSerialNumber;
    }

    public String getXmpOriginalDocumentID() {
        return xmpOriginalDocumentID;
    }

    public void setXmpOriginalDocumentID(String xmpOriginalDocumentID) {
        this.xmpOriginalDocumentID = xmpOriginalDocumentID;
    }

    public String getXmpRights() {
        return xmpRights;
    }

    public void setXmpRights(String xmpRights) {
        this.xmpRights = xmpRights;
    }

    public String getXmpSerialNumber() {
        return xmpSerialNumber;
    }

    public void setXmpSerialNumber(String xmpSerialNumber) {
        this.xmpSerialNumber = xmpSerialNumber;
    }

    public String getXmpTitle() {
        return xmpTitle;
    }

    public void setXmpTitle(String xmpTitle) {
        this.xmpTitle = xmpTitle;
    }

    public String getXmpWebStatement() {
        return xmpWebStatement;
    }

    public void setXmpWebStatement(String xmpWebStatement) {
        this.xmpWebStatement = xmpWebStatement;
    }

    public Float getCompositeMegaPixels() {
        return compositeMegaPixels;
    }

    public void setCompositeMegaPixels(Float compositeMegaPixels) {
        this.compositeMegaPixels = compositeMegaPixels;
    }

    public String getAvailNasaId() {
        return availNasaId;
    }

    public void setAvailNasaId(String availNasaId) {
        this.availNasaId = availNasaId;
    }

    public String getAvailCenter() {
        return availCenter;
    }

    public void setAvailCenter(String availCenter) {
        this.availCenter = availCenter;
    }

    public String getAvailDescription() {
        return availDescription;
    }

    public void setAvailDescription(String availDescription) {
        this.availDescription = availDescription;
    }

    public String getAvailDescription508() {
        return availDescription508;
    }

    public void setAvailDescription508(String availDescription508) {
        this.availDescription508 = availDescription508;
    }

    public String getAvailLocation() {
        return availLocation;
    }

    public void setAvailLocation(String availLocation) {
        this.availLocation = availLocation;
    }

    public String getAvailPhotographer() {
        return availPhotographer;
    }

    public void setAvailPhotographer(String availPhotographer) {
        this.availPhotographer = availPhotographer;
    }

    public String getAvailSecondaryCreator() {
        return availSecondaryCreator;
    }

    public void setAvailSecondaryCreator(String availSecondaryCreator) {
        this.availSecondaryCreator = availSecondaryCreator;
    }

    public String getAvailTitle() {
        return availTitle;
    }

    public void setAvailTitle(String availTitle) {
        this.availTitle = availTitle;
    }

    @JsonIgnore
    public Stream<String> getCopyrights() {
        return Arrays.asList(exifCopyright, xmpRights).stream().filter(StringUtils::isNotBlank).distinct();
    }

    @JsonIgnore
    public Stream<String> getPhotographers() {
        return Arrays.asList(availPhotographer, availSecondaryCreator, exifArtist, iptcByLine).stream()
                .filter(StringUtils::isNotBlank)
                .distinct();
    }

    @Override
    public String toString() {
        return "ExifMetadata [id=" + id + "]";
    }

    public static ExifMetadata of(Metadata metadata) {
        ExifMetadata result = new ExifMetadata();

        ofNullable(metadata.getFirstDirectoryOfType(FileTypeDirectory.class)).ifPresent(file -> {
            result.setFileFileType(file.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_NAME));
            result.setFileFileTypeExtension(file.getDescription(FileTypeDirectory.TAG_EXPECTED_FILE_NAME_EXTENSION));
            result.setFileMimeType(file.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE));
        });

        ofNullable(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)).ifPresent(ifd0 -> {
            result.setExifArtist(ifd0.getDescription(ExifDirectoryBase.TAG_ARTIST));
            result.setExifCopyright(ifd0.getDescription(ExifDirectoryBase.TAG_COPYRIGHT));
            ofNullable(ifd0.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT))
                    .ifPresent(result::setExifExifImageHeight);
            ofNullable(ifd0.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH))
                    .ifPresent(result::setExifExifImageWidth);
            result.setExifImageDescription(ifd0.getDescription(ExifDirectoryBase.TAG_IMAGE_DESCRIPTION));
            ofNullable(ifd0.getInteger(ExifDirectoryBase.TAG_IMAGE_HEIGHT)).ifPresent(result::setExifImageHeight);
            ofNullable(ifd0.getInteger(ExifDirectoryBase.TAG_IMAGE_WIDTH)).ifPresent(result::setExifImageWidth);
        });

        ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class)).ifPresent(subifd -> {
            result.setExifLensSerialNumber(subifd.getDescription(ExifDirectoryBase.TAG_LENS_SERIAL_NUMBER));
            result.setExifSerialNumber(subifd.getDescription(ExifDirectoryBase.TAG_BODY_SERIAL_NUMBER));
        });

        ofNullable(metadata.getFirstDirectoryOfType(IptcDirectory.class)).map(IptcDescriptor::new).ifPresent(iptc -> {
            result.setIptcByLine(iptc.getByLineDescription());
            result.setIptcCaptionAbstract(iptc.getCaptionDescription());
            result.setIptcCopyrightNotice(iptc.getCopyrightNoticeDescription());
            result.setIptcObjectName(iptc.getObjectNameDescription());
            result.setIptcWriterEditor(iptc.getWriterDescription());
        });

        ofNullable(metadata.getFirstDirectoryOfType(PhotoshopDirectory.class))
                .ifPresent(ps -> ofNullable(ps.getDescription(PhotoshopDirectory.TAG_URL)).map(Utils::newURL)
                        .ifPresent(result::setPhotoshopUrl));

        ofNullable(metadata.getFirstDirectoryOfType(XmpDirectory.class)).map(XmpDirectory::getXmpProperties)
                .ifPresent(xmp -> {
                    result.setXmpCaptionWriter(xmp.get("photoshop:CaptionWriter"));
                    result.setXmpDescription(xmp.get("dc:description[1]"));
                    result.setXmpFormat(xmp.get("dc:format"));
                    result.setXmpLensSerialNumber(xmp.get("aux:LensSerialNumber"));
                    result.setXmpOriginalDocumentID(xmp.get("xmpMM:OriginalDocumentID"));
                    result.setXmpRights(xmp.get("dc:rights[1]"));
                    result.setXmpSerialNumber(xmp.get("aux:SerialNumber"));
                    result.setXmpTitle(xmp.get("dc:title[1]"));
                    ofNullable(xmp.get("xmpRights:WebStatement")).ifPresent(result::setXmpWebStatement);
        });

        return result;
    }
}
