package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
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
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchiveQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.MetaQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.rest.UserProfile;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryPageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CommonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsService.class);

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

    @Value("${commons.api.oauth.access.token}")
    private String oauthAccessToken;

    @Value("${commons.cat.search.depth}")
    private int catSearchDepth;

    @Value("${commons.img.preview.width}")
    private int imgPreviewWidth;

    private CloseableHttpClient httpClient;
    private String token;

    @PostConstruct
    public void init() throws IOException {
        httpClient = createHttpClient();
        token = queryToken();
        UserProfile userProfile = restApiHttpGet("/oauth2/resource/profile", UserProfile.class);
        LOGGER.info("Identified to Wikimedia Commons API as {}", userProfile.getUserName());
        if (userProfile.isBlocked()) {
            LOGGER.warn("Wikimedia Commons user account is blocked!");
        }
        if (!userProfile.getRights().contains("upload")) {
            LOGGER.warn("Wikimedia Commons user account has no upload right!");
        }
    }

    @PreDestroy
    public void destroy() throws IOException {
        httpClient.close();
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

    public String queryToken() throws IOException {
        return apiHttpGet("?action=query&meta=tokens", MetaQueryResponse.class).getQuery().getTokens().getCsrftoken();
    }

    public List<FileArchive> queryFileArchive(String sha1base36) throws IOException {
        return apiHttpGet("?action=query&list=filearchive&fasha1base36=" + sha1base36,
                FileArchiveQueryResponse.class).getQuery().getFilearchive();
    }

    /**
     * Creates an HTTP client smart enough to understand WMF cookies. See
     * <a href="https://stackoverflow.com/a/40697322/2257172">Fixing HttpClient
     * warning “Invalid expires attribute” using fluent API</a>
     * 
     * @return an HTTP client smart enough to understand WMF cookies
     */
    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();
    }

    private <T> T apiHttpGet(String path, Class<T> responseClass) throws IOException {
        return httpGet(apiUrl + path + "&format=json", responseClass);
    }

    private <T> T restApiHttpGet(String path, Class<T> responseClass) throws IOException {
        return httpGet(restApiUrl + path, responseClass);
    }

    private <T> T httpGet(String url, Class<T> responseClass) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + oauthAccessToken);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return jackson.readValue(getHttpResponseBody(response), responseClass);
        }
    }

    private HttpPost apiHttpPost(List<NameValuePair> nvps) {
        HttpPost httpPost = new HttpPost(apiUrl.toExternalForm());
        nvps.add(new BasicNameValuePair("Authorization", "Bearer " + oauthAccessToken));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return httpPost;
    }

    private static String getHttpResponseBody(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        Header encoding = entity.getContentEncoding();
        String body = IOUtils.toString(entity.getContent(),
                encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding.getValue()));
        EntityUtils.consume(entity);
        return body;
    }

    public String getWikiHtmlPreview(String wikiCode, String pageTitle) throws IOException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("action", "visualeditor"));
        nvps.add(new BasicNameValuePair("format", "json"));
        nvps.add(new BasicNameValuePair("formatversion", "2"));
        nvps.add(new BasicNameValuePair("paction", "parsedoc"));
        nvps.add(new BasicNameValuePair("page", pageTitle));
        nvps.add(new BasicNameValuePair("wikitext", wikiCode));
        nvps.add(new BasicNameValuePair("pst", "true"));
        HttpPost httpPost = apiHttpPost(nvps);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String body = getHttpResponseBody(response);
            VisualEditorResponse apiResponse = jackson.readValue(body, VeApiResponse.class).getVisualeditor();
            if (!"success".equals(apiResponse.getResult())) {
                throw new IllegalArgumentException(apiResponse.toString());
            }
            return apiResponse.getContent();
        }
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
            Element list = self.isHiddenCategory(category) ? hiddenCatLinksList : normalCatLinksList;
            Element item = new Element("li");
            list.appendChild(item);
            Utils.appendChildElement(item, "a", category.replace('_', ' '),
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
     * Determines if a Commons category is hidden, using the special
     * {@code __HIDDENCAT__} behavior switch. See <a href=
     * "https://www.mediawiki.org/wiki/Help:Magic_words#Behavior_switches">documentation</a>.
     * 
     * @param category category to check
     * @return {@code true} if the category is hidden
     * @throws CategoryNotFoundException if the category is not found
     * @throws CategoryPageNotFoundException if no page is found for the category
     */
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

    @Cacheable("categoryPages")
    public CommonsPage getCategoryPage(String category) {
        return pageRepository.findByCategoryTitle(categoryRepository
                .findByTitle(category.replace(' ', '_')).orElseThrow(() -> new CategoryNotFoundException(category)).getTitle())
                .orElseThrow(() -> new CategoryPageNotFoundException(category));
    }

    @Cacheable("subCategories")
    public Set<String> getSubCategories(String category) {
        return categoryLinkRepository
                .findByTypeAndIdTo(CommonsCategoryLinkType.subcat, category.replace(' ', '_'))
                .stream().map(c -> c.getId().getFrom().getTitle()).collect(Collectors.toSet());
    }

    @Cacheable("subCategoriesByDepth")
    public Set<String> getSubCategories(String category, int depth) {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.debug("Fetching '{}' subcategories with depth {}...", category, depth);
        Set<String> subcats = self.getSubCategories(category);
        Set<String> result = subcats.stream().map(s -> s.replace('_', ' '))
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        if (depth > 0) {
            subcats.parallelStream().forEach(s -> result.addAll(self.getSubCategories(s, depth - 1)));
        }
        LOGGER.debug("Fetching '{}' subcategories with depth {} completed in {}", category, depth,
                Duration.between(LocalDateTime.now(), start));
        return result;
    }

    public Set<String> cleanupCategories(Set<String> categories) {
        LocalDateTime start = LocalDateTime.now();
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
                Duration.between(LocalDateTime.now(), start));
        if (!categories.isEmpty() && result.isEmpty()) {
            throw new IllegalStateException("Cleaning " + categories + " removed all categories!");
        }
        return result;
    }

    public static String formatWikiCode(String badWikiCode) {
        return badWikiCode.replaceAll("<a [^>]*href=\"([^\"]*)\"[^>]*>([^<]*)</a>", "[$1 $2]");
    }

    public void upload(String wikiCode, String filename, URL url) throws IOException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("action", "upload"));
        nvps.add(new BasicNameValuePair("comment",
                "#Spacemedia - Upload of " + url + " via https://tools.wmflabs.org/spacemedia/"));
        nvps.add(new BasicNameValuePair("format", "json"));
        nvps.add(new BasicNameValuePair("filename", Objects.requireNonNull(filename, "filename")));
        nvps.add(new BasicNameValuePair("ignorewarnings", "1"));
        nvps.add(new BasicNameValuePair("text", wikiCode));
        nvps.add(new BasicNameValuePair("token", token));
        nvps.add(new BasicNameValuePair("url", url.toExternalForm()));
        HttpPost httpPost = apiHttpPost(nvps);
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String body = getHttpResponseBody(response);
            LOGGER.info(body);
            UploadApiResponse apiResponse = new ObjectMapper().readValue(body, UploadApiResponse.class);
            UploadError error = apiResponse.getError();
            if (error != null) {
                throw new IllegalArgumentException(error.toString());
            }
            if (!"success".equals(apiResponse.getUpload().getResult())) {
                throw new IllegalArgumentException(apiResponse.toString());
            }
        }
    }

    static class UploadApiResponse {
        private UploadResponse upload;
        private UploadError error;
        @JsonProperty("servedby")
        private String servedBy;

        public UploadResponse getUpload() {
            return upload;
        }

        public void setUpload(UploadResponse upload) {
            this.upload = upload;
        }

        public UploadError getError() {
            return error;
        }

        public void setError(UploadError error) {
            this.error = error;
        }

        public String getServedBy() {
            return servedBy;
        }

        public void setServedBy(String servedBy) {
            this.servedBy = servedBy;
        }
    }

    static class UploadResponse {
        private String result;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    static class UploadError {
        private String code;
        private String info;
        @JsonProperty("*")
        private String star;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public String getStar() {
            return star;
        }

        public void setStar(String star) {
            this.star = star;
        }

        @Override
        public String toString() {
            return "UploadError [" + (code != null ? "code=" + code + ", " : "")
                    + (info != null ? "info=" + info + ", " : "")
                    + (star != null ? "*=" + star : "")
                    + "]";
        }
    }
}
