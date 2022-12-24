package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

public class ParseApiResponse extends ApiResponse {

    private ParseResponse parse;

    public ParseResponse getParse() {
        return parse;
    }

    public void setParse(ParseResponse parse) {
        this.parse = parse;
    }

    @Override
    public String toString() {
        return "ParseApiResponse [" + (parse != null ? "parse=" + parse + ", " : "")
                + (getError() != null ? "error=" + getError() + ", " : "")
                + (getServedBy() != null ? "servedBy=" + getServedBy() : "") + "]";
    }
}
