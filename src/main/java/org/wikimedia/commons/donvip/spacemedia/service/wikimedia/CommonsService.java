package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.TemporalQueries.localDate;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkId;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryLinkType;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsCategoryRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageProjection;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRestrictionsRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.ApiError;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.EditApiResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchive;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchiveQuery;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.FileArchiveQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Limit;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.MetaQuery;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.MetaQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Revision;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.RevisionsPage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.RevisionsQuery;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.RevisionsQueryResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Slot;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.Tokens;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UploadApiResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UploadResponse;
import org.wikimedia.commons.donvip.spacemedia.data.commons.api.UserInfo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeData;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryPageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.ExecutionMode;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;
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

    private static final String DUPLICATE = "Duplicate";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsService.class);

    private static final String COMMONS = "commons";

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
    private CommonsPageRestrictionsRepository restrictionsRepository;

    @Autowired
    private HashAssociationRepository hashRepository;

    @Autowired
    protected RuntimeDataRepository runtimeDataRepository;

    @Autowired
    private ObjectMapper jackson;

    /**
     * Self-autowiring to call {@link Cacheable} methods, otherwise the cache is
     * skipped. Spring cache is only trigerred on external calls.
     */
    @Resource
    private CommonsService self;

    @Autowired
    private RemoteService remote;

    @Value("${commons.api.url}")
    private URL apiUrl;

    @Value("${commons.api.rest.url}")
    private URL restApiUrl;

    @Value("${commons.duplicate.url}")
    private URL duplicateUrl;

    @Value("${commons.duplicates.max.files}")
    private long duplicateMaxFiles;

    @Value("${commons.ignored.duplicates.sha1}")
    private Set<String> ignoredDuplicatesSha1;

    @Value("${commons.ignored.duplicates.name}")
    private Set<String> ignoredDuplicatesName;

    @Value("${commons.ignored.duplicates.cats}")
    private Set<String> ignoredDuplicatesCategories;

    @Value("${commons.cat.search.depth}")
    private int catSearchDepth;

    @Value("${commons.img.preview.width}")
    private int imgPreviewWidth;

    @Value("${commons.permitted.file.types}")
    private Set<String> permittedFileTypes;

    @Value("${execution.mode}")
    private ExecutionMode hashMode;

    @Value("${threads.number:8}")
    private int threadsNumber;

    @Value("${commons.ignore.query.user.info.error:false}")
    private boolean ignoreQueryUserInfoError;

    @Value("${commons.dpla.max.duplicates}")
    private int dplaMaxDuplicates;

    @Value("${commons.automatic.hashes.computation.asc:false}")
    private boolean automaticHashComputationAsc;

    @Value("${commons.automatic.hashes.computation.desc:false}")
    private boolean automaticHashComputationDesc;

    @Autowired
    private ExecutorService taskExecutor;

    private final String account;
    private final String userAgent;
    private final OAuth10aService oAuthService;
    private final OAuth1AccessToken oAuthAccessToken;

    private UserInfo userInfo;
    private String token;
    private LocalDateTime lastUpload;

    private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd000000");

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
        LOGGER.info("CSRF token: {}", token);
        if (automaticHashComputationAsc) {
            taskExecutor.submit(this::computeHashesOfAllFilesAsc);
        }
        if (automaticHashComputationDesc) {
            taskExecutor.submit(this::computeHashesOfAllFilesDesc);
        }
    }

    private boolean hasUploadByUrlRight() {
        return userInfo.getRights().contains("upload_by_url");
    }

    private boolean hasUploadRight() {
        return userInfo.getRights().contains("upload");
    }

    public Set<String> findFilesWithSha1(String sha1) throws IOException {
        return findFilesWithSha1(List.of(sha1));
    }

    private static void displayUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        LOGGER.info("Memory used: {}. Free: {}. Total: {}.",
                FileUtils.byteCountToDisplaySize(total - free),
                FileUtils.byteCountToDisplaySize(free),
                FileUtils.byteCountToDisplaySize(total));
    }

    /**
     * Finds files in Wikimedia Commons by their SHA-1 hash.
     *
     * @param sha1s SHA-1 hashes, can be either in base 36 (31 characters) or base
     *              16 (40 characters)
     * @return Set of Commons file names matching the given SHA-1 hashes
     * @throws IOException in case of I/O error
     */
    public Set<String> findFilesWithSha1(Collection<String> sha1s) throws IOException {
        // See https://www.mediawiki.org/wiki/Manual:Image_table#img_sha1
        // The SHA-1 hash of the file contents in base 36 format, zero-padded to 31 characters
        Set<String> sha1base36 = sha1s.stream()
                .map(sha1 -> sha1.length() == 31 ? sha1 : base36Sha1(sha1))
                .collect(toSet());
        Set<String> files = imageRepository.findBySha1InOrderByTimestamp(sha1base36).stream()
                .map(CommonsImageProjection::getName)
                .collect(toSet());
        if (files.isEmpty()) {
            files.addAll(
                    oldImageRepository.findBySha1In(sha1base36).stream().map(CommonsOldImage::getName)
                            .collect(toSet()));
        }
        if (files.isEmpty()) {
            for (String s : sha1base36) {
                files.addAll(queryFileArchive(s).stream().map(FileArchive::getName).collect(toSet()));
            }
        }
        return files;
    }

    /**
     * Converts a SHA-1 into base 36 SHA-1 used by Commons.
     *
     * @param sha1 SHA-1 hash
     * @return base 36 SHA-1 used by Commons
     */
    public static String base36Sha1(String sha1) {
        return String.format("%31s", new BigInteger(sha1, 16).toString(36)).replace(' ', '0');
    }

    public synchronized Tokens queryTokens() throws IOException {
        return metaQuery("?action=query&meta=tokens", Tokens.class, MetaQuery::getTokens);
    }

    public UserInfo queryUserInfo() throws IOException {
        return metaQuery("?action=query&meta=userinfo&uiprop=blockinfo|groups|rights|ratelimits", UserInfo.class,
                MetaQuery::getUserInfo);
    }

    private <T> T metaQuery(String path, Class<T> resultClass, Function<MetaQuery, T> extractor) throws IOException {
        MetaQueryResponse response = apiHttpGet(path, MetaQueryResponse.class);
        if (response.getError() != null) {
            if (ignoreQueryUserInfoError) {
                try {
                    return resultClass.getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IOException(e);
                }
            } else {
                throw new IOException(response.getError().toString());
            }
        }
        return extractor.apply(response.getQuery());
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
        RevisionsQuery query = apiHttpGet("?action=query&prop=revisions&rvprop=content&rvslots=main&rvlimit=1&pageids=" + pageId,
                RevisionsQueryResponse.class).getQuery();
        if (query != null) {
            RevisionsPage rp = query.getPages().get(pageId);
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
        }
        LOGGER.error("Couldn't find page content for {}", pageId);
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
                .collect(toSet());
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
                .map(c -> c.getFrom().getTitle().replace('_', ' ')).collect(toSet());
    }

    @Cacheable("subCategoriesByDepth")
    public Set<String> getSubCategories(String category, int depth) {
        LocalDateTime start = now();
        LOGGER.debug("Fetching '{}' subcategories with depth {}...", category, depth);
        Set<String> subcats = self.getSubCategories(category);
        Set<String> result = subcats.stream().collect(toCollection(ConcurrentHashMap::newKeySet));
        if (depth > 0) {
            subcats.parallelStream().forEach(s -> result.addAll(self.getSubCategories(s, depth - 1)));
        }
        LOGGER.debug("Fetching '{}' subcategories with depth {} completed in {}", category, depth,
                Duration.between(now(), start).truncatedTo(ChronoUnit.SECONDS));
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
        Set<String> lowerCategories = categories.stream().map(c -> c.toLowerCase(Locale.ENGLISH)).collect(toSet());
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
                Duration.between(now(), start).truncatedTo(ChronoUnit.SECONDS));
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
        return doUpload(wikiCode, normalizeFilename(filename), url, sha1, true, true, true);
    }

    public static String normalizeFilename(String filename) {
        // replace forbidden chars, see https://www.mediawiki.org/wiki/Manual:$wgIllegalFileChars
        return filename.replace('/', '-').replace(':', '-').replace('\\', '-').replace('.', '_').replace("&amp;", "&")
                .trim();
    }

    public boolean isPermittedFileType(String url) {
        String lowerCaseUrl = url.toLowerCase(Locale.ENGLISH);
        return permittedFileTypes.stream().anyMatch(type -> lowerCaseUrl.endsWith("." + type));
    }

    private synchronized String doUpload(String wikiCode, String filename, URL url, String sha1,
            boolean renewTokenIfBadToken, boolean retryWithSanitizedUrl, boolean retryAfterRandomProxy403error)
            throws IOException, UploadException {
        if (!isPermittedFileType(url.toExternalForm())) {
            throw new UploadException(url + " does not match any supported file type: " + permittedFileTypes);
        }
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
        ApiError error = apiResponse.getError();
        if (error != null) {
            if (renewTokenIfBadToken && "badtoken".equals(error.getCode())) {
                token = queryTokens().getCsrftoken();
                return doUpload(wikiCode, filename, url, sha1, false, retryWithSanitizedUrl, true);
            }
            if (retryWithSanitizedUrl && "http-invalid-url".equals(error.getCode())) {
                try {
                    return doUpload(wikiCode, filename, Utils.urlToUri(url).toURL(), sha1, renewTokenIfBadToken, false,
                            true);
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
            if (retryAfterRandomProxy403error && "http-curl-error".equals(error.getCode())
                    && "Error fetching URL: Received HTTP code 403 from proxy after CONNECT".equals(error.getInfo())) {
                return doUpload(wikiCode, filename, url, sha1, renewTokenIfBadToken, true, false);
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

    public void checkExactDuplicateFiles() throws IOException {
        LOGGER.info("Looking for duplicate files in Commons...");
        LocalDateTime start = LocalDateTime.now();
        long currentDupes = categoryLinkRepository.countByTypeAndIdTo(CommonsCategoryLinkType.file, DUPLICATE);
        LOGGER.info("There are currently {} duplicate files identified as such in Commons", currentDupes);
        if (currentDupes >= duplicateMaxFiles) {
            LOGGER.warn("Too much backlog, skipping");
            return;
        }
        int count = 0;
        LOGGER.info("Querying complete list of Special:ListDuplicatedFiles...");
        Elements items = Jsoup.connect(duplicateUrl.toExternalForm() + "&limit=5000").maxBodySize(0).get()
                .getElementsByClass("special").first().getElementsByTag("li");
        LOGGER.info("Special:ListDuplicatedFiles returned {} entries", items.size());
        Collections.reverse(items);
        for (Element li : items) {
            String title = li.getElementsByTag("a").first().ownText().replace(' ', '_');
            CommonsImageProjection image = findImage(title);
            if (image != null
                    && (CommonsMediaType.AUDIO == image.getMediaType()
                            || (image.getWidth() > 1 && image.getHeight() > 1))
                    && !ignoredDuplicatesSha1.contains(image.getSha1())) {
                boolean dpla = title.contains("-_DPLA_-");
                List<CommonsImageProjection> duplicates = imageRepository.findBySha1OrderByTimestamp(image.getSha1());
                int numberOfFiles = duplicates.size();
                try {
                    if (numberOfFiles > 1 && (!dpla || numberOfFiles < dplaMaxDuplicates)
                            && duplicates.stream().noneMatch(
                                    d -> ignoredDuplicatesName.contains(d.getName()) || self.isInIgnoredCategory(d))) {
                        CommonsImageProjection olderImage = duplicates.get(0);
                        for (int i = 1; i < duplicates.size(); i++) {
                            count += handleDuplicateFile(olderImage, duplicates.get(i), count);
                        }
                        if (currentDupes + count >= duplicateMaxFiles) {
                            break;
                        }
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to handle {}", title, e);
                }
            }
        }
        LOGGER.info("{} duplicate files handled in {}", count,
                Duration.between(start, LocalDateTime.now()).truncatedTo(ChronoUnit.SECONDS));
    }

    @Transactional(transactionManager = "commonsTransactionManager")
    public boolean isInIgnoredCategory(CommonsImageProjection d) {
        return categoryLinkRepository
                .findByIdFrom(pageRepository.findByFileTitle(d.getName())
                        .orElseThrow(() -> new IllegalStateException("No page named " + d.getName())))
                .stream()
                .anyMatch(c -> ignoredDuplicatesCategories.stream()
                        .anyMatch(x -> c.getId().getTo().startsWith(x.replace(' ', '_'))));
    }

    private int handleDuplicateFile(CommonsImageProjection olderImage, CommonsImageProjection dupe, int count)
            throws IOException {
        CommonsPage dupePage = pageRepository.findByFileTitle(dupe.getName())
                .orElseThrow(() -> new IllegalStateException("No page named " + dupe.getName()));
        if (!categoryLinkRepository
                .existsById(new CommonsCategoryLinkId(dupePage, DUPLICATE))
                && !restrictionsRepository.existsByPageAndType(dupePage, "edit")
                && count < duplicateMaxFiles
                && categoryLinkRepository.countByTypeAndIdTo(CommonsCategoryLinkType.file,
                        DUPLICATE) < duplicateMaxFiles
                && !oldImageRepository.existsByName(dupe.getName())) {
            return edit(Map.of("action", "edit", "title", "File:" + dupe.getName(), "format", "json",
                    "summary", "Duplicate of [[:File:" + olderImage.getName() + "]]", "prependtext",
                    "{{duplicate|" + olderImage.getName() + "}}\n", "token", token), false);
        }
        return 0;
    }

    private int edit(Map<String, String> params, boolean retryAttempt) throws IOException {
        EditApiResponse response = apiHttpPost(params, EditApiResponse.class);
        if (response.getEdit() == null || response.getError() != null
                || !"Success".equalsIgnoreCase(response.getEdit().getResult())) {
            if ("badtoken".equals(response.getError().getCode())) {
                LOGGER.error("API rejected our CSRF token {}", token);
                // Renew it and try again once
                if (!retryAttempt) {
                    LOGGER.info("Renewing token and retrying...");
                    token = queryTokens().getCsrftoken();
                    return edit(params, true);
                }
            }
            throw new IllegalStateException(response.toString());
        }
        return 1;
    }

    private CommonsImageProjection findImage(String title) {
        Optional<CommonsImageProjection> imageOpt = imageRepository.findByName(title);
        if (imageOpt.isEmpty()) {
            // Check if it's a redirect
            CommonsPage page = pageRepository.findByFileTitle(title).orElse(null);
            if (page == null) {
                // Probably already deleted
                return null;
            } else if (Boolean.TRUE.equals(page.getIsRedirect())) {
                title = page.getRedirect().getTitle();
                imageOpt = imageRepository.findByName(title);
            }
        }
        return imageOpt.orElse(null);
    }

    public void computeHashesOfAllFilesAsc() {
        computeHashesOfAllFiles(Direction.ASC);
    }

    public void computeHashesOfAllFilesDesc() {
        computeHashesOfAllFiles(Direction.DESC);
    }

    private void computeHashesOfAllFiles(Direction order) {
        Thread.currentThread().setName("commons-computeHashesOfAllFiles-" + order);
        LOGGER.info("Computing perceptual hashes of files in Commons ({} order)...", order);
        displayUsedMemory();
        final RuntimeData runtime = runtimeDataRepository.findById(COMMONS).orElseGet(() -> new RuntimeData(COMMONS));
        LOGGER.info("Runtime data: {}", runtime);
        final long startingTimestamp = Long.parseLong(hashMode == ExecutionMode.LOCAL
                ? Optional.ofNullable(runtime.getLastTimestamp()).orElse("20010101000000")
                : remote.getHashLastTimestamp());
        final long endingTimestamp = Long.parseLong(ZonedDateTime.now(ZoneId.of("UTC")).format(timestampFormatter));
        final LocalDate startingDate = timestampFormatter.parse(Long.toString(startingTimestamp), localDate());
        final long days = ChronoUnit.DAYS.between(startingDate,
                timestampFormatter.parse(Long.toString(endingTimestamp), localDate()));
        final String startingTimestampString = startingDate.format(dateFormatter);
        final String endingTimestampString = startingDate.plusDays(days + 1).format(dateFormatter);
        if (hashMode == ExecutionMode.LOCAL) {
            computeHashesOfFilesLocal(order, runtime, startingTimestampString, endingTimestampString);
        } else {
            computeHashesOfFilesRemote(order, startingTimestampString, endingTimestampString);
        }
    }

    private void computeHashesOfFilesLocal(Direction order, RuntimeData runtime, String startingTimestamp,
            String endingTimestamp) {
        final LocalDateTime start = LocalDateTime.now();
        Page<CommonsImageProjection> page = null;
        String lastTimestamp = null;
        int hashCount = 0;
        int pageIndex = 0;
        LOGGER.info("Locally computing perceptual hashes of files in Commons ({} order) between {} and {}", order,
                startingTimestamp, endingTimestamp);
        do {
            page = fetchPage(order, startingTimestamp, endingTimestamp, pageIndex++);
            for (CommonsImageProjection image : page.getContent()) {
                hashCount += computeAndSaveHash(image);
                lastTimestamp = image.getTimestamp();
                if (Direction.ASC == order) {
                    runtime.setLastTimestamp(lastTimestamp);
                    runtimeDataRepository.save(runtime);
                }
            }
            LOGGER.info(
                    "{} perceptual hashes ({} order) computed in {} ({} pages). Current timestamp: {}",
                    hashCount, order, Duration.between(start, LocalDateTime.now()).truncatedTo(ChronoUnit.SECONDS),
                    pageIndex, lastTimestamp);
        } while (page.hasNext());
    }

    private void computeHashesOfFilesRemote(Direction order, String startingTimestamp, String endingTimestamp) {
        Page<CommonsImageProjection> page = null;
        int pageIndex = 0;
        LOGGER.info("Remotely computing perceptual hashes ({} order) between {} and {}", order,
                startingTimestamp, endingTimestamp);
        do {
            page = fetchPage(order, startingTimestamp, endingTimestamp, pageIndex++);
            List<CommonsImageProjection> content = page.getContent();
            if (!content.isEmpty()) {
                LOGGER.info("Scheduling remote computation of page {}", pageIndex);
                taskExecutor.submit(() -> {
                    LOGGER.info("Starting remote computation of new page. First item date is {}",
                            content.get(0).getTimestamp());
                    int hashCount = 0;
                    for (CommonsImageProjection image : content) {
                        hashCount += computeAndSaveHash(image);
                    }
                    LOGGER.info("Finished remote computation of page (+{} hashes). Last item date is {}", hashCount,
                            content.get(content.size() - 1).getTimestamp());
                });
            }
        } while (page.hasNext());
    }

    private Page<CommonsImageProjection> fetchPage(Direction order, String startingTimestamp, String endingTimestamp,
            int pageIndex) {
        displayUsedMemory();
        Page<CommonsImageProjection> result = imageRepository.findByMinorMimeInAndTimestampBetween(
                Set.of("gif", "jpeg", "png", "tiff", "webp"),
                startingTimestamp, endingTimestamp, PageRequest.of(pageIndex, 1000, order, "timestamp"));
        displayUsedMemory();
        return result;
    }

    private int computeAndSaveHash(CommonsImageProjection image) {
        if (!hashRepository.existsById(image.getSha1())) {
            BufferedImage bi = null;
            try {
                URL url = getImageUrl(image.getName());
                bi = Utils.readImage(url, false, false);
                if (bi == null) {
                    throw new IOException("Failed to read image from " + url);
                }
                HashAssociation hash = hashRepository.save(
                        new HashAssociation(image.getSha1(), HashHelper.encode(HashHelper.computePerceptualHash(bi))));
                if (hashMode == ExecutionMode.REMOTE) {
                    remote.putHashAssociation(hash);
                }
                return 1;
            } catch (IOException | URISyntaxException | ImageDecodingException | RuntimeException e) {
                LOGGER.error("Failed to compute/save hash of {}: {}", image, e.toString());
            } finally {
                if (bi != null) {
                    bi.flush();
                }
            }
        }
        return 0;
    }

    protected static URL getImageUrl(String imageName) throws MalformedURLException {
        // https://www.mediawiki.org/wiki/Manual:$wgHashedUploadDirectory
        String md5 = DigestUtils.md5Hex(imageName);
        // https://www.mediawiki.org/wiki/Manual:PAGENAMEE_encoding#Encodings_compared
        // algo used: urlencode(WIKI) Incompatible with Java UrlEncoder.encode
        String encodedFilename = imageName.replace(' ', '_').replace("%", "%25").replace("\"", "%22")
                .replace("#", "%23").replace("&", "%26").replace("'", "%27").replace("+", "%2B").replace("<", "%3C")
                .replace("=", "%3D").replace(">", "%3E").replace("?", "%3F").replace("[", "%5B").replace("\\", "%5C")
                .replace("]", "%5D").replace("^", "%5E").replace("`", "%60").replace("{", "%7B").replace("|", "%7C")
                .replace("}", "%7D").replace(" ", "%C2%A0").replace("¡", "%C2%A1");
        return new URL(String.format("https://upload.wikimedia.org/wikipedia/commons/%c/%s/%s", md5.charAt(0),
                md5.substring(0, 2), encodedFilename));
    }
}
