package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.net.URL;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageInfo {

    private ZonedDateTime timestamp;

    private String user;

    @JsonProperty("userid")
    private Long userId;

    private Long size;

    private Integer width;

    private Integer height;

    private String comment;

    @JsonProperty("parsedcomment")
    private String parsedComment;

    private String html;

    @JsonProperty("canonicaltitle")
    private String canonicalTitle;

    private URL url;

    @JsonProperty("descriptionurl")
    private URL descriptionUrl;

    @JsonProperty("descriptionshorturl")
    private URL descriptionShortUrl;

    private String sha1;

    private String mime;

    private String mediatype;

    @JsonProperty("bitdepth")
    private Short bitDepth;

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getParsedComment() {
        return parsedComment;
    }

    public void setParsedComment(String parsedComment) {
        this.parsedComment = parsedComment;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getCanonicalTitle() {
        return canonicalTitle;
    }

    public void setCanonicalTitle(String canonicalTitle) {
        this.canonicalTitle = canonicalTitle;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getDescriptionUrl() {
        return descriptionUrl;
    }

    public void setDescriptionUrl(URL descriptionUrl) {
        this.descriptionUrl = descriptionUrl;
    }

    public URL getDescriptionShortUrl() {
        return descriptionShortUrl;
    }

    public void setDescriptionShortUrl(URL descriptionShortUrl) {
        this.descriptionShortUrl = descriptionShortUrl;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public String getMediatype() {
        return mediatype;
    }

    public void setMediatype(String mediatype) {
        this.mediatype = mediatype;
    }

    public Short getBitDepth() {
        return bitDepth;
    }

    public void setBitDepth(Short bitDepth) {
        this.bitDepth = bitDepth;
    }

    @Override
    public String toString() {
        return "ImageInfo [" + (timestamp != null ? "timestamp=" + timestamp + ", " : "")
                + (user != null ? "user=" + user + ", " : "") + (userId != null ? "userId=" + userId + ", " : "")
                + (size != null ? "size=" + size + ", " : "") + (width != null ? "width=" + width + ", " : "")
                + (height != null ? "height=" + height + ", " : "")
                + (comment != null ? "comment=" + comment + ", " : "")
                + (parsedComment != null ? "parsedComment=" + parsedComment + ", " : "")
                + (canonicalTitle != null ? "canonicalTitle=" + canonicalTitle + ", " : "")
                + (url != null ? "url=" + url + ", " : "")
                + (descriptionUrl != null ? "descriptionUrl=" + descriptionUrl + ", " : "")
                + (descriptionShortUrl != null ? "descriptionShortUrl=" + descriptionShortUrl + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 + ", " : "") + (mime != null ? "mime=" + mime + ", " : "")
                + (mediatype != null ? "mediatype=" + mediatype + ", " : "")
                + (bitDepth != null ? "bitDepth=" + bitDepth : "") + "]";
    }
}
