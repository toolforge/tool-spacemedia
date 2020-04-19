package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateLimits {

    private RateLimit move;
    private RateLimit edit;
    private RateLimit upload;

    @JsonProperty("linkpurge")
    private RateLimit linkPurge;

    @JsonProperty("badcaptcha")
    private RateLimit badCaptcha;

    @JsonProperty("emailuser")
    private RateLimit emailUser;

    @JsonProperty("changeemail")
    private RateLimit changeEmail;

    private RateLimit rollback;
    private RateLimit purge;

    @JsonProperty("renderfile")
    private RateLimit renderFile;

    @JsonProperty("renderfile-nonstandard")
    private RateLimit renderFileNonStandard;

    @JsonProperty("cxsave")
    private RateLimit cxSave;

    @JsonProperty("urlshortcode")
    private RateLimit urlShortCode;

    @JsonProperty("thanks-notification")
    private RateLimit thanksNotification;

    @JsonProperty("badoath")
    private RateLimit badOath;

    public RateLimit getMove() {
        return move;
    }

    public void setMove(RateLimit move) {
        this.move = move;
    }

    public RateLimit getEdit() {
        return edit;
    }

    public void setEdit(RateLimit edit) {
        this.edit = edit;
    }

    public RateLimit getUpload() {
        return upload;
    }

    public void setUpload(RateLimit upload) {
        this.upload = upload;
    }

    public RateLimit getLinkPurge() {
        return linkPurge;
    }

    public void setLinkPurge(RateLimit linkPurge) {
        this.linkPurge = linkPurge;
    }

    public RateLimit getBadCaptcha() {
        return badCaptcha;
    }

    public void setBadCaptcha(RateLimit badCaptcha) {
        this.badCaptcha = badCaptcha;
    }

    public RateLimit getEmailUser() {
        return emailUser;
    }

    public void setEmailUser(RateLimit emailUser) {
        this.emailUser = emailUser;
    }

    public RateLimit getChangeEmail() {
        return changeEmail;
    }

    public void setChangeEmail(RateLimit changeEmail) {
        this.changeEmail = changeEmail;
    }

    public RateLimit getRollback() {
        return rollback;
    }

    public void setRollback(RateLimit rollback) {
        this.rollback = rollback;
    }

    public RateLimit getPurge() {
        return purge;
    }

    public void setPurge(RateLimit purge) {
        this.purge = purge;
    }

    public RateLimit getRenderFile() {
        return renderFile;
    }

    public void setRenderFile(RateLimit renderFile) {
        this.renderFile = renderFile;
    }

    public RateLimit getRenderFileNonStandard() {
        return renderFileNonStandard;
    }

    public void setRenderFileNonStandard(RateLimit renderFileNonStandard) {
        this.renderFileNonStandard = renderFileNonStandard;
    }

    public RateLimit getCxSave() {
        return cxSave;
    }

    public void setCxSave(RateLimit cxSave) {
        this.cxSave = cxSave;
    }

    public RateLimit getUrlShortCode() {
        return urlShortCode;
    }

    public void setUrlShortCode(RateLimit urlShortCode) {
        this.urlShortCode = urlShortCode;
    }

    public RateLimit getThanksNotification() {
        return thanksNotification;
    }

    public void setThanksNotification(RateLimit thanksNotification) {
        this.thanksNotification = thanksNotification;
    }

    public RateLimit getBadOath() {
        return badOath;
    }

    public void setBadOath(RateLimit badOath) {
        this.badOath = badOath;
    }
}
