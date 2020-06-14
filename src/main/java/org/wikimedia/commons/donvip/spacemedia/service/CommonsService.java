package org.wikimedia.commons.donvip.spacemedia.service;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkId;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkType;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchive;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchiveQuery;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchiveQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Limit;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.MetaQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Revision;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.RevisionsPage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.RevisionsQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Slot;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Tokens;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UploadApiResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UploadError;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UploadResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UserInfo;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryPageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MediaWikiApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Service
public class CommonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsService.class);

    private static final Pattern EXACT_DUPE_ERROR = Pattern.compile(
            "The upload is an exact duplicate of the current version of \\[\\[:File:(.+)\\]\\]\\.");

    /**
     * Minimal delay between successive uploads, in seconds.
     */
    private static final int DELAY = 5;

    @Autowired
    private CommonsImageRepository imageRepository;

    @Autowired
    private CommonsOldImageRepository oldImageRepository;

    @Autowired
    private CommonsCategoryRepository categoryRepository;

    @Autowired
    private CommonsPageRepository pageRepository;

    @Autowired
    private CommonsCategoryLinkRepository categoryLinkRepository;

    @Autowired
    private ObjectMapper jackson;

    /**
     * Self-autowiring to call {@link Cacheable} methods, otherwise the cache is
     * skipped. Spring cache is only trigerred on external calls.
     */
    @Resource
    private CommonsService self;

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

    public CommonsService(
            @Value("${application.version}") String appVersion,
            @Value("${application.contact}") String appContact,
            @Value("${flickr4java.version}") String flickr4javaVersion,
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
                "Spacemedia", appVersion, appContact, apiAccount, "SpringBoot", bootVersion, "ScribeJava",
                scribeVersion, "Flickr4Java", flickr4javaVersion);
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

    public Set<String> findFilesWithSha1(String sha1) throws IOException {
        // See https://www.mediawiki.org/wiki/Manual:Image_table#img_sha1
        // The SHA-1 hash of the file contents in base 36 format, zero-padded to 31 characters
        String sha1base36 = String.format("%31s", new BigInteger(sha1, 16).toString(36)).replace(' ', '0');
        Set<String> files = imageRepository.findBySha1(sha1base36).stream().map(CommonsImage::getName).collect(Collectors.toSet());
        if (files.isEmpty()) {
            files.addAll(oldImageRepository.findBySha1(sha1base36).stream().map(CommonsOldImage::getName).collect(Collectors.toSet()));
        }
        if (files.isEmpty()) {
            files.addAll(queryFileArchive(sha1base36).stream().map(FileArchive::getName).collect(Collectors.toSet()));
        }
        return files;
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
        VisualEditorResponse apiResponse = apiHttpPost(Map.of(
                "action", "visualeditor",
                "format", "json",
                "formatversion", "2",
                "paction", "parsedoc",
                "page", pageTitle,
                "wikitext", wikiCode,
                "pst", "true"
        ), VeApiResponse.class).getVisualeditor();

        if (!"success".equals(apiResponse.getResult())) {
            throw new IllegalArgumentException(apiResponse.toString());
        }
        return apiResponse.getContent();
    }

    public String getWikiHtmlPreview(String wikiCode, String pageTitle, String imgUrl) throws IOException {
        Document doc = Jsoup.parse(getWikiHtmlPreview(wikiCode, pageTitle));
        Element body = doc.getElementsByTag("body").get(0);
        // Display image
        Element imgLink = Utils.prependChildElement(body, "a", null, Map.of("href", imgUrl));
        Utils.appendChildElement(imgLink, "img", null,
                Map.of("src", imgUrl, "width", Integer.toString(imgPreviewWidth)));
        // Display categories
        Element lastSection = body.getElementsByTag("section").last();
        Element catLinksDiv = Utils.appendChildElement(lastSection, "div", null,
                Map.of("id", "catlinks", "class", "catlinks", "data-mw", "interface"));
        Element normalCatLinksDiv = Utils.appendChildElement(catLinksDiv, "div", null,
                Map.of("id", "mw-normal-catlinks", "class", "mw-normal-catlinks"));
        Utils.appendChildElement(normalCatLinksDiv, "a", "Categories",
                Map.of("href", "https://commons.wikimedia.org/wiki/Special:Categories", "title", "Special:Categories"));
        normalCatLinksDiv.appendText(": ");
        Element normalCatLinksList = new Element("ul");
        normalCatLinksDiv.appendChild(normalCatLinksList);
        Element hiddenCatLinksList = new Element("ul");
        Utils.appendChildElement(catLinksDiv, "div", "Hidden categories: ",
                Map.of("id", "mw-hidden-catlinks", "class", "mw-hidden-catlinks mw-hidden-cats-user-shown"))
                .appendChild(hiddenCatLinksList);
        for (Element link : lastSection.getElementsByTag("link")) {
            String category = link.attr("href").replace("#" + pageTitle.replace(" ", "%20"), "").replace("./Category:", "");
            String href = "https://commons.wikimedia.org/wiki/Category:" + category;
            Element list = normalCatLinksList;
            try {
                list = self.isHiddenCategory(category) ? hiddenCatLinksList : normalCatLinksList;
            } catch (CategoryNotFoundException | CategoryPageNotFoundException e) {
                LOGGER.warn("Category/page not found: {}", e.getMessage());
            }
            Element item = new Element("li");
            list.appendChild(item);
            Utils.appendChildElement(item, "a", sanitizeCategory(category),
                    Map.of("href", href, "title", "Category:" + category));
            link.remove();
        }
        return doc.toString();
    }

    static class VeApiResponse {
        private VisualEditorResponse visualeditor;

        public VisualEditorResponse getVisualeditor() {
            return visualeditor;
        }

        public void setVisualeditor(VisualEditorResponse visualeditor) {
            this.visualeditor = visualeditor;
        }
    }

    static class VisualEditorResponse {
        private String result;
        private String content;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "VisualEditorResponse [" + (result != null ? "result=" + result + ", " : "")
                    + (content != null ? "content=" + content : "") + "]";
        }
    }

    /**
     * Returns the API bot account name. Used for User-Agent and Commons categories.
     *
     * @return the API bot account name
     */
    public String getAccount() {
        return account;
    }

    /**
     * Determines if a Commons category is hidden, using the special
     * {@code __HIDDENCAT__} behavior switch. See <a href=
     * "https://www.mediawiki.org/wiki/Help:Magic_words#Behavior_switches">documentation</a>.
     *
     * @param category category to check
     * @return {@code true} if the category is hidden
     * @throws CategoryNotFoundException     if the category is not found
     * @throws CategoryPageNotFoundException if no page is found for the category
     */
    @Transactional(transactionManager = "commonsTransactionManager")
    @Cacheable("hiddenCategories")
    public boolean isHiddenCategory(String category) {
        return self.getCategoryPage(category).getProps().stream().anyMatch(pp -> "hiddencat".equals(pp.getPropname()));
    }

    /**
     * Determines if a Commons category exists and is not a redirect.
     *
     * @param category category to check
     * @return {@code true} if the category exists and is not a redirect
     */
    @Cacheable("upToDateCategories")
    public boolean isUpToDateCategory(String category) {
        try {
            return self.getCategoryPage(category).getRedirect() == null;
        } catch (CategoryNotFoundException | CategoryPageNotFoundException e) {
            return false;
        }
    }

    public Set<String> findNonUpToDateCategories(Collection<String> categories) {
        return categories.parallelStream()
            .flatMap(s -> Arrays.stream(s.split(";")))
            .filter(c -> !c.isEmpty() && !self.isUpToDateCategory(c))
            .collect(Collectors.toSet());
    }

    private static String sanitizeCategory(String category) {
        return category.replace(' ', '_').split("#")[0];
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    @Cacheable("categoryPages")
    public CommonsPage getCategoryPage(String category) {
        return pageRepository.findByCategoryTitle(categoryRepository
                .findByTitle(sanitizeCategory(category))
                    .orElseThrow(() -> new CategoryNotFoundException(category)).getTitle())
                .orElseThrow(() -> new CategoryPageNotFoundException(category));
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    @Cacheable("subCategories")
    public Set<String> getSubCategories(String category) {
        return categoryLinkRepository
                .findIdByTypeAndIdTo(CommonsCategoryLinkType.subcat, sanitizeCategory(category)).stream()
                .map(c -> c.getFrom().getTitle()).collect(Collectors.toSet());
    }

    @Cacheable("subCategoriesByDepth")
    public Set<String> getSubCategories(String category, int depth) {
        LocalDateTime start = now();
        LOGGER.debug("Fetching '{}' subcategories with depth {}...", category, depth);
        Set<String> subcats = self.getSubCategories(category);
        Set<String> result = subcats.stream().map(CommonsService::sanitizeCategory)
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        if (depth > 0) {
            subcats.parallelStream().forEach(s -> result.addAll(self.getSubCategories(s, depth - 1)));
        }
        LOGGER.debug("Fetching '{}' subcategories with depth {} completed in {}", category, depth,
                Duration.between(now(), start));
        return result;
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    @Cacheable("filesInCategory")
    public Set<CommonsCategoryLinkId> getFilesInCategory(String category) {
        return categoryLinkRepository
                .findIdByTypeAndIdTo(CommonsCategoryLinkType.file, sanitizeCategory(category));
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    @Cacheable("filesPageInCategory")
    public Page<CommonsCategoryLinkId> getFilesInCategory(String category, Pageable page) {
        return categoryLinkRepository
                .findIdByTypeAndIdTo(CommonsCategoryLinkType.file, sanitizeCategory(category), page);
    }

    public String getPageContent(CommonsPage page) throws IOException {
        return queryRevisionContent(page.getId());
    }

    public Set<String> cleanupCategories(Set<String> categories) {
        LocalDateTime start = now();
        LOGGER.info("Cleaning {} categories with depth {}...", categories.size(), catSearchDepth);
        Set<String> result = new HashSet<>();
        Set<String> lowerCategories = categories.stream().map(c -> c.toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toSet());
        for (Iterator<String> it = categories.iterator(); it.hasNext();) {
            String c = it.next().toLowerCase(Locale.ENGLISH);
            if (c.endsWith("s")) {
                c = c.substring(0, c.length() - 1);
            }
            final String fc = c;
            // Quickly remove instances of rockets, spacecraft, satellites and so on
            if (lowerCategories.stream().anyMatch(lc -> lc.contains("(" + fc + ")"))) {
                it.remove();
            }
        }
        for (String cat : categories) {
            Set<String> subcats = self.getSubCategories(cat, catSearchDepth);
            if (subcats.parallelStream().noneMatch(categories::contains)) {
                result.add(cat);
            }
        }
        LOGGER.info("Cleaning {} categories with depth {} completed in {}", categories.size(), catSearchDepth,
                Duration.between(now(), start));
        if (!categories.isEmpty() && result.isEmpty()) {
            throw new IllegalStateException("Cleaning " + categories + " removed all categories!");
        }
        // Make sure all imported files get reviewed
        result.add("Spacemedia files (review needed)");
        return result;
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
                    return doUpload(wikiCode, filename, Utils.urlToUri(url).toURL(), sha1, renewTokenIfBadToken, false);
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
