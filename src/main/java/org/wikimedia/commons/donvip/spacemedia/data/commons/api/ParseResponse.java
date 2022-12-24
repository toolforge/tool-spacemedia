package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParseResponse {

    @JsonProperty("title")
    private String title;

    @JsonProperty("pageid")
    private long pageId;

    @JsonProperty("headhtml")
    private String headHtml;

    @JsonProperty("text")
    private String text;

    @JsonProperty("categorieshtml")
    private String categoriesHtml;

    @JsonProperty("displaytitle")
    private String displayTitle;

    @JsonProperty("subtitle")
    private String subtitle;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public String getHeadHtml() {
        return headHtml;
    }

    public void setHeadHtml(String headHtml) {
        this.headHtml = headHtml;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCategoriesHtml() {
        return categoriesHtml;
    }

    public void setCategoriesHtml(String categoriesHtml) {
        this.categoriesHtml = categoriesHtml;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    @Override
    public String toString() {
        return "ParseResponse [" + (title != null ? "title=" + title + ", " : "") + "pageId=" + pageId + ", "
                + (headHtml != null ? "headHtml=" + headHtml + ", " : "")
                + (text != null ? "text=" + text + ", " : "")
                + (categoriesHtml != null ? "categoriesHtml=" + categoriesHtml + ", " : "")
                + (displayTitle != null ? "displayTitle=" + displayTitle + ", " : "")
                + (subtitle != null ? "subtitle=" + subtitle : "") + "]";
    }
}
