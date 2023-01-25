package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikiPage {

    @JsonProperty("pageid")
    private long pageId;

    private int ns;

    private String title;

    private long size;

    @JsonProperty("wordcount")
    private int wordCount;

    private String snippet;

    private ZonedDateTime timestamp;

    private int index;

    @JsonProperty("contentmodel")
    private String contentModel;

    @JsonProperty("pagelanguage")
    private String pageLanguage;

    @JsonProperty("pagelanguagehtmlcode")
    private String pageLanguageHtmlCode;

    @JsonProperty("pagelanguagedir")
    private String pageLanguageDir;

    private ZonedDateTime touched;

    @JsonProperty("lastrevid")
    private long lastRevId;

    private int length;

    @JsonProperty("fullurl")
    private URL fullUrl;

    @JsonProperty("editurl")
    private URL editUrl;

    @JsonProperty("canonicalurl")
    private URL canonicalUrl;

    @JsonProperty("imagerepository")
    private String imageRepository;

    @JsonProperty("imageinfo")
    private ImageInfo[] imageInfo;

    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public int getNs() {
        return ns;
    }

    public void setNs(int ns) {
        this.ns = ns;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getContentModel() {
        return contentModel;
    }

    public void setContentModel(String contentModel) {
        this.contentModel = contentModel;
    }

    public String getPageLanguage() {
        return pageLanguage;
    }

    public void setPageLanguage(String pageLanguage) {
        this.pageLanguage = pageLanguage;
    }

    public String getPageLanguageHtmlCode() {
        return pageLanguageHtmlCode;
    }

    public void setPageLanguageHtmlCode(String pageLanguageHtmlCode) {
        this.pageLanguageHtmlCode = pageLanguageHtmlCode;
    }

    public String getPageLanguageDir() {
        return pageLanguageDir;
    }

    public void setPageLanguageDir(String pageLanguageDir) {
        this.pageLanguageDir = pageLanguageDir;
    }

    public ZonedDateTime getTouched() {
        return touched;
    }

    public void setTouched(ZonedDateTime touched) {
        this.touched = touched;
    }

    public long getLastRevId() {
        return lastRevId;
    }

    public void setLastRevId(long lastRevId) {
        this.lastRevId = lastRevId;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public URL getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(URL fullUrl) {
        this.fullUrl = fullUrl;
    }

    public URL getEditUrl() {
        return editUrl;
    }

    public void setEditUrl(URL editUrl) {
        this.editUrl = editUrl;
    }

    public URL getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(URL canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public String getImageRepository() {
        return imageRepository;
    }

    public void setImageRepository(String imageRepository) {
        this.imageRepository = imageRepository;
    }

    public ImageInfo[] getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo[] imageInfo) {
        this.imageInfo = imageInfo;
    }

    @Override
    public String toString() {
        return "WikiPage [pageId=" + pageId + ", ns=" + ns + ", " + (title != null ? "title=" + title + ", " : "")
                + "size=" + size + ", wordCount=" + wordCount + ", "
                + (snippet != null ? "snippet=" + snippet + ", " : "")
                + (timestamp != null ? "timestamp=" + timestamp + ", " : "") + "index=" + index + ", "
                + (contentModel != null ? "contentModel=" + contentModel + ", " : "")
                + (pageLanguage != null ? "pageLanguage=" + pageLanguage + ", " : "")
                + (pageLanguageHtmlCode != null ? "pageLanguageHtmlCode=" + pageLanguageHtmlCode + ", " : "")
                + (pageLanguageDir != null ? "pageLanguageDir=" + pageLanguageDir + ", " : "")
                + (touched != null ? "touched=" + touched + ", " : "") + "lastRevId=" + lastRevId + ", length=" + length
                + ", " + (fullUrl != null ? "fullUrl=" + fullUrl + ", " : "")
                + (editUrl != null ? "editUrl=" + editUrl + ", " : "")
                + (canonicalUrl != null ? "canonicalUrl=" + canonicalUrl + ", " : "")
                + (imageRepository != null ? "imageRepository=" + imageRepository + ", " : "")
                + (imageInfo != null ? "imageInfo=" + Arrays.toString(imageInfo) : "") + "]";
    }
}
