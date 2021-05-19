package org.wikimedia.commons.donvip.spacemedia.commons;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.FileArchive;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.FileArchiveQuery;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.FileArchiveQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.Limit;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.MetaQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.Revision;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.RevisionsPage;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.RevisionsQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.Slot;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.Tokens;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.UploadApiResponse;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.UploadError;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.UploadResponse;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.UserInfo;
import org.wikimedia.commons.donvip.spacemedia.commons.api.exceptions.UploadException;
import org.wikimedia.commons.donvip.spacemedia.commons.veapi.VeApiResponse;
import org.wikimedia.commons.donvip.spacemedia.commons.veapi.VisualEditorResponse;
import org.wikimedia.commons.donvip.spacemedia.utils.UriUrlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MediaWikiApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Service
public class CommonsApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsApiService.class);

    private static final Pattern EXACT_DUPE_ERROR = Pattern.compile(
            "The upload is an exact duplicate of the current version of \\[\\[:File:(.+)\\]\\]\\.");

    /**
     * Minimal delay between successive uploads, in seconds.
     */
    private static final int DELAY = 5;

    @Autowired
    private ObjectMapper jackson;

    @Value("${commons.api.url}")
    private URL apiUrl;

    @Value("${commons.api.rest.url}")
    private URL restApiUrl;

    @Value("${commons.cat.search.depth}")
    private int catSearchDepth;

    @Value("${commons.img.preview.width}")
    private int imgPreviewWidth;

    private final String account;
    private final String userAgent;
    private final OAuth10aService oAuthService;
    private final OAuth1AccessToken oAuthAccessToken;

    private UserInfo userInfo;
    private String token;
    private LocalDateTime lastUpload;

    public CommonsApiService(
            @Value("${application.version}") String appVersion,
            @Value("${application.contact}") String appContact,
            @Value("${spring-boot.version}") String bootVersion,
            @Value("${scribejava.version}") String scribeVersion,
            @Value("${commons.api.account}") String apiAccount,
            @Value("${commons.api.oauth1.consumer-token}") String consumerToken,
            @Value("${commons.api.oauth1.consumer-secret}") String consumerSecret,
            @Value("${commons.api.oauth1.access-token}") String accessToken,
            @Value("${commons.api.oauth1.access-secret}") String accessSecret
    ) {
        account = apiAccount;
        // Comply to Wikimedia User-Agent Policy: https://meta.wikimedia.org/wiki/User-Agent_policy
        if (!account.toLowerCase(Locale.ENGLISH).contains("bot")) {
            throw new IllegalArgumentException("Bot account must include 'bot' in its name!");
        }
        userAgent = String.format("%s/%s (%s - %s) %s/%s %s/%s %s/%s",
                "Spacemedia", appVersion, appContact, apiAccount, "SpringBoot", bootVersion, "ScribeJava", scribeVersion);
        oAuthService = new ServiceBuilder(consumerToken).apiSecret(consumerSecret).build(MediaWikiApi.instance());
        oAuthAccessToken = new OAuth1AccessToken(accessToken, accessSecret);
    }

    @PostConstruct
    public void init() throws IOException {
        userInfo = queryUserInfo();
        LOGGER.info("Identified to Wikimedia Commons API as {}", userInfo.getName());
        if (!hasUploadRight() && !hasUploadByUrlRight()) {
            LOGGER.warn("Wikimedia Commons user account has no upload right!");
        }
        if (userInfo.getRateLimits() != null && userInfo.getRateLimits().getUpload() != null) {
            Limit uploadRate = userInfo.getRateLimits().getUpload().getUser();
            LOGGER.info("Upload rate limited to {} hits every {} seconds.", uploadRate.getHits(), uploadRate.getSeconds());
        } else {
            LOGGER.warn("Cannot retrieve upload rate for Wikimedia Commons user account!");
        }
        // Fetch CSRF token, mandatory for upload using the Mediawiki API
        token = queryTokens().getCsrftoken();
    }

    private boolean hasUploadByUrlRight() {
        return userInfo.getRights().contains("upload_by_url");
    }

    private boolean hasUploadRight() {
        return userInfo.getRights().contains("upload");
    }

    public synchronized Tokens queryTokens() throws IOException {
        return apiHttpGet("?action=query&meta=tokens", MetaQueryResponse.class).getQuery().getTokens();
    }

    public UserInfo queryUserInfo() throws IOException {
        return apiHttpGet("?action=query&meta=userinfo&uiprop=blockinfo|groups|rights|ratelimits",
                MetaQueryResponse.class).getQuery().getUserInfo();
    }

    public List<FileArchive> queryFileArchive(String sha1base36) throws IOException {
        FileArchiveQuery query = apiHttpGet("?action=query&list=filearchive&fasha1base36=" + sha1base36,
                FileArchiveQueryResponse.class).getQuery();
        return query != null ? query.getFilearchive() : Collections.emptyList();
    }

    private <T> T apiHttpGet(String path, Class<T> responseClass) throws IOException {
        return httpGet(apiUrl.toExternalForm() + path + "&format=json", responseClass);
    }

    private <T> T apiHttpPost(Map<String, String> params, Class<T> responseClass) throws IOException {
        return httpPost(apiUrl.toExternalForm(), responseClass, params);
    }

    private <T> T httpGet(String url, Class<T> responseClass) throws IOException {
        return httpCall(Verb.GET, url, responseClass, Collections.emptyMap(), Collections.emptyMap(), true);
    }

    private <T> T httpPost(String url, Class<T> responseClass, Map<String, String> params) throws IOException {
        return httpCall(Verb.POST, url, responseClass,
                Map.of("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"), params, true);
    }

    private <T> T httpCall(Verb verb, String url, Class<T> responseClass, Map<String, String> headers,
            Map<String, String> params, boolean retryOnTimeout) throws IOException {
        OAuthRequest request = new OAuthRequest(verb, url);
        request.setCharset(StandardCharsets.UTF_8.name());
        params.forEach(request::addParameter);
        headers.forEach(request::addHeader);
        request.addHeader("User-Agent", userAgent);
        oAuthService.signRequest(oAuthAccessToken, request);
        try {
            String body = oAuthService.execute(request).getBody();
            if ("upstream request timeout".equalsIgnoreCase(body)) {
                if (retryOnTimeout) {
                    return httpCall(verb, url, responseClass, headers, params, false);
                } else {
                    throw new IOException(body);
                }
            }
            return jackson.readValue(body, responseClass);
        } catch (SocketTimeoutException e) {
            if (retryOnTimeout) {
                return httpCall(verb, url, responseClass, headers, params, false);
            } else {
                throw e;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public String queryRevisionContent(int pageId) throws IOException {
        RevisionsPage rp = apiHttpGet("?action=query&prop=revisions&rvprop=content&rvslots=main&rvlimit=1&pageids=" + pageId,
                RevisionsQueryResponse.class).getQuery().getPages().get(pageId);
        if (rp != null) {
            List<Revision> revisions = rp.getRevisions();
            if (CollectionUtils.isNotEmpty(revisions)) {
                Map<String, Slot> slots = revisions.get(0).getSlots();
                if (MapUtils.isNotEmpty(slots)) {
                    Slot main = slots.get("main");
                    if (main != null) {
                        return main.getContent();
                    }
                }
            }
        }
        LOGGER.error("Couldn't find page content for {}: {}", pageId, rp);
        return null;
    }

    public String getWikiHtmlPreview(String wikiCode, String pageTitle) throws IOException {
        VeApiResponse apiResponse = apiHttpPost(Map.of(
                "action", "visualeditor",
                "format", "json",
                "formatversion", "2",
                "paction", "parsedoc",
                "page", pageTitle,
                "wikitext", wikiCode,
                "pst", "true"
        ), VeApiResponse.class);
        if (apiResponse.getError() != null) {
            throw new IllegalArgumentException(apiResponse.getError().toString());
        }

        VisualEditorResponse veResponse = apiResponse.getVisualeditor();
        if (!"success".equals(veResponse.getResult())) {
            throw new IllegalArgumentException(veResponse.toString());
        }
        return veResponse.getContent();
    }

    /**
     * Returns the API bot account name. Used for User-Agent and Commons categories.
     *
     * @return the API bot account name
     */
    public String getAccount() {
        return account;
    }

    public static String formatWikiCode(String badWikiCode) {
        return badWikiCode.replaceAll("<a [^>]*href=\"([^\"]*)\"[^>]*>([^<]*)</a>", "[$1 $2]");
    }

    public String upload(String wikiCode, String filename, URL url, String sha1) throws IOException, UploadException {
        return doUpload(wikiCode, normalizeFilename(filename), url, sha1, true, true);
    }

    public String normalizeFilename(String filename) {
        // replace forbidden chars, see https://www.mediawiki.org/wiki/Manual:$wgIllegalFileChars
        return filename.replace('/', '-').replace(':', '-').replace('\\', '-').replace('.', '_');
    }

    private synchronized String doUpload(String wikiCode, String filename, URL url, String sha1,
            boolean renewTokenIfBadToken, boolean retryWithSanitizedUrl)
            throws IOException, UploadException {
        Map<String, String> params = new HashMap<>(Map.of(
                "action", "upload",
                "comment", "#Spacemedia - Upload of " + url + " via [[:Commons:Spacemedia]]",
                "format", "json",
                "filename", Objects.requireNonNull(filename, "filename"),
                "ignorewarnings", "1",
                "text", Objects.requireNonNull(wikiCode, "wikiCode"),
                "token", token
        ));
        if (hasUploadByUrlRight()) {
            params.put("url", url.toExternalForm());
        } else {
            throw new UnsupportedOperationException("Application is not yet able to upload by file, only by URL");
        }

        ensureUploadRate();

        LOGGER.info("Uploading {} as {}..", url, filename);
        UploadApiResponse apiResponse = apiHttpPost(params, UploadApiResponse.class);
        LOGGER.info("Upload of {} as {}: {}", url, filename, apiResponse);
        UploadResponse upload = apiResponse.getUpload();
        UploadError error = apiResponse.getError();
        if (error != null) {
            if (renewTokenIfBadToken && "badtoken".equals(error.getCode())) {
                token = queryTokens().getCsrftoken();
                return doUpload(wikiCode, filename, url, sha1, false, retryWithSanitizedUrl);
            }
            if (retryWithSanitizedUrl && "http-invalid-url".equals(error.getCode())) {
                try {
                    return doUpload(wikiCode, filename, UriUrlUtils.urlToUri(url).toURL(), sha1, renewTokenIfBadToken, false);
                } catch (URISyntaxException e) {
                    throw new UploadException(error.getCode(), e);
                }
            }
            if ("fileexists-no-change".equals(error.getCode())) {
                Matcher m = EXACT_DUPE_ERROR.matcher(error.getInfo());
                if (m.matches()) {
                    return m.group(1);
                }
            }
            throw new UploadException(error.toString());
        } else if (!"Success".equals(upload.getResult())) {
            throw new UploadException(apiResponse.toString());
        }
        if (!sha1.equalsIgnoreCase(upload.getImageInfo().getSha1())) {
            throw new IllegalStateException(String.format(
                    "SHA1 mismatch for %s ! Expected %s, got %s", url, sha1, upload.getImageInfo().getSha1()));
        }
        return upload.getFilename();
    }

    private void ensureUploadRate() throws UploadException {
        LocalDateTime fiveSecondsAgo = now().minusSeconds(DELAY);
        if (lastUpload != null && lastUpload.isAfter(fiveSecondsAgo)) {
            try {
                Thread.sleep(DELAY - SECONDS.between(now(), lastUpload.plusSeconds(DELAY)));
            } catch (InterruptedException e) {
                throw new UploadException(e);
            }
        }
        lastUpload = now();
    }
}
