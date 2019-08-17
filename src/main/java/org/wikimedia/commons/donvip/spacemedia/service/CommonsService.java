package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsFileArchive;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsFileArchiveRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsOldImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsPageRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryPageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CommonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsService.class);

    @Autowired
    private CommonsImageRepository imageRepository;

    @Autowired
    private CommonsFileArchiveRepository fileArchiveRepository;

    @Autowired
    private CommonsOldImageRepository oldImageRepository;

    @Autowired
    private CommonsCategoryRepository categoryRepository;

    @Autowired
    private CommonsPageRepository pageRepository;

    @Autowired
    private CommonsCategoryLinkRepository categoryLinkRepository;

    /**
     * Self-autowiring to call {@link Cacheable} methods, otherwise the cache is
     * skipped. Spring cache is only trigerred on external calls.
     */
    @Resource
    private CommonsService self;

    @Value("${commons.api.url}")
    private URL apiUrl;

    @Value("${commons.cat.search.depth}")
    private int catSearchDepth;

    /**
     * Configurable because of a major performance bug on the database replica (the
     * field is no indexed, see
     * <a href="https://phabricator.wikimedia.org/T71088">#T71088</a>
     */
    @Value("${commons.query.filearchive}")
    private boolean queryFilearchive;

    public Set<String> findFilesWithSha1(String sha1) {
        // See https://www.mediawiki.org/wiki/Manual:Image_table#img_sha1
        // The SHA-1 hash of the file contents in base 36 format, zero-padded to 31 characters 
        String sha1base36 = String.format("%31s", new BigInteger(sha1, 16).toString(36)).replace(' ', '0');
        Set<String> files = imageRepository.findBySha1(sha1base36).stream().map(CommonsImage::getName).collect(Collectors.toSet());
        if (files.isEmpty()) {
            files.addAll(oldImageRepository.findBySha1(sha1base36).stream().map(CommonsOldImage::getName).collect(Collectors.toSet()));
        }
        if (files.isEmpty() && queryFilearchive) {
            files.addAll(fileArchiveRepository.findBySha1(sha1base36).stream().map(CommonsFileArchive::getName).collect(Collectors.toSet()));
        }
        return files;
    }

    public String getWikiHtmlPreview(String wikiCode, String pageTitle) throws ClientProtocolException, IOException {
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().disableCookieManagement().build()) {
            HttpPost httpPost = new HttpPost(apiUrl.toExternalForm());
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("action", "visualeditor"));
            nvps.add(new BasicNameValuePair("format", "json"));
            nvps.add(new BasicNameValuePair("formatversion", "2"));
            nvps.add(new BasicNameValuePair("paction", "parsedoc"));
            nvps.add(new BasicNameValuePair("page", pageTitle));
            nvps.add(new BasicNameValuePair("wikitext", wikiCode));
            nvps.add(new BasicNameValuePair("pst", "true"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                Header encoding = entity.getContentEncoding();
                String body = IOUtils.toString(entity.getContent(),
                        encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding.getValue()));
                EntityUtils.consume(entity);
                VisualEditorResponse apiResponse = new ObjectMapper().readValue(body, ApiResponse.class)
                        .getVisualeditor();
                if (!"success".equals(apiResponse.getResult())) {
                    throw new IllegalArgumentException(apiResponse.toString());
                }
                return apiResponse.getContent();
            }
        }
    }

    @SuppressWarnings("serial")
    public String getWikiHtmlPreview(String wikiCode, String pageTitle, String imgUrl)
            throws ClientProtocolException, IOException {
        Document doc = Jsoup.parse(getWikiHtmlPreview(wikiCode, pageTitle));
        Element body = doc.getElementsByTag("body").get(0);
        // Display image
        Element imgLink = Utils.prependChildElement(body, "a", null, new HashMap<String, String>() {
            {
                put("href", imgUrl);
            }
        });
        Utils.appendChildElement(imgLink, "img", null, new HashMap<String, String>() {
            {
                put("src", imgUrl);
                put("width", "800");
            }
        });
        // Display categories
        Element lastSection = body.getElementsByTag("section").last();
        Element catLinksDiv = Utils.appendChildElement(lastSection, "div", null, new HashMap<String, String>() {
            {
                put("id", "catlinks");
                put("class", "catlinks");
                put("data-mw", "interface");
            }
        });
        Element normalCatLinksDiv = Utils.appendChildElement(catLinksDiv, "div", null, new HashMap<String, String>() {
            {
                put("id", "mw-normal-catlinks");
                put("class", "mw-normal-catlinks");
            }
        });
        Utils.appendChildElement(normalCatLinksDiv, "a", "Categories", new HashMap<String, String>() {
            {
                put("href", "https://commons.wikimedia.org/wiki/Special:Categories");
                put("title", "Special:Categories");
            }
        });
        normalCatLinksDiv.appendText(": ");
        Element normalCatLinksList = new Element("ul");
        normalCatLinksDiv.appendChild(normalCatLinksList);
        Element hiddenCatLinksList = new Element("ul");
        Utils.appendChildElement(catLinksDiv, "div", "Hidden categories: ", new HashMap<String, String>() {
            {
                put("id", "mw-hidden-catlinks");
                put("class", "mw-hidden-catlinks mw-hidden-cats-user-shown");
            }
        }).appendChild(hiddenCatLinksList);
        for (Element link : lastSection.getElementsByTag("link")) {
            String category = link.attr("href").replace("#" + pageTitle.replace(" ", "%20"), "").replace("./Category:", "");
            String href = "https://commons.wikimedia.org/wiki/Category:" + category;
            Element list = self.isHiddenCategory(category) ? hiddenCatLinksList : normalCatLinksList;
            Element item = new Element("li");
            list.appendChild(item);
            Utils.appendChildElement(item, "a", category.replace('_', ' '), new HashMap<String, String>() {
                {
                    put("href", href);
                    put("title", "Category:" + category);
                }
            });
            link.remove();
        }
        return doc.toString();
    }

    static class ApiResponse {
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

    @Cacheable("categoryPages")
    public CommonsPage getCategoryPage(String category) {
        return pageRepository.findByCategoryTitle(categoryRepository
                .findByTitle(category).orElseThrow(() -> new CategoryNotFoundException(category)).getTitle())
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
        Set<String> LowerCategories = categories.stream().map(c -> c.toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toSet());
        for (Iterator<String> it = categories.iterator(); it.hasNext();) {
            String c = it.next().toLowerCase(Locale.ENGLISH);
            if (c.endsWith("s")) {
                c = c.substring(0, c.length() - 1);
            }
            final String fc = c;
            // Quickly remove instances of rockets, spacecraft, satellites and so on
            if (LowerCategories.stream().anyMatch(lc -> lc.contains("(" + fc + ")"))) {
                it.remove();
            }
        }
        for (String cat : categories) {
            Set<String> subcats = self.getSubCategories(cat, catSearchDepth);
            if (subcats.parallelStream().noneMatch(c -> categories.contains(c))) {
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
}
