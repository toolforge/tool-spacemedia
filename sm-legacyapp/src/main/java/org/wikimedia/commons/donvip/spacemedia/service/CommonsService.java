package org.wikimedia.commons.donvip.spacemedia.service;

import static java.time.LocalDateTime.now;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

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
import org.wikimedia.commons.donvip.spacemedia.commons.CommonsApiService;
import org.wikimedia.commons.donvip.spacemedia.commons.api.data.FileArchive;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsCategoryLinkId;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsCategoryLinkRepository;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsCategoryLinkType;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsCategoryRepository;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsImage;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsOldImage;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsOldImageRepository;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsPage;
import org.wikimedia.commons.donvip.spacemedia.commons.data.jpa.CommonsPageRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.CategoryPageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class CommonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonsService.class);

    @Autowired
    private CommonsApiService apiService;

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

    /**
     * Self-autowiring to call {@link Cacheable} methods, otherwise the cache is
     * skipped. Spring cache is only trigerred on external calls.
     */
    @Resource
    private CommonsService self;

    @Value("${commons.cat.search.depth}")
    private int catSearchDepth;

    @Value("${commons.img.preview.width}")
    private int imgPreviewWidth;

    public Set<String> findFilesWithSha1(String sha1) throws IOException {
        // See https://www.mediawiki.org/wiki/Manual:Image_table#img_sha1
        // The SHA-1 hash of the file contents in base 36 format, zero-padded to 31 characters
        String sha1base36 = String.format("%31s", new BigInteger(sha1, 16).toString(36)).replace(' ', '0');
        Set<String> files = imageRepository.findBySha1(sha1base36).stream().map(CommonsImage::getName).collect(Collectors.toSet());
        if (files.isEmpty()) {
            files.addAll(oldImageRepository.findBySha1(sha1base36).stream().map(CommonsOldImage::getName).collect(Collectors.toSet()));
        }
        if (files.isEmpty()) {
            files.addAll(apiService.queryFileArchive(sha1base36).stream().map(FileArchive::getName).collect(Collectors.toSet()));
        }
        return files;
    }

    public String getWikiHtmlPreview(String wikiCode, String pageTitle, String imgUrl) throws IOException {
        Document doc = Jsoup.parse(apiService.getWikiHtmlPreview(wikiCode, pageTitle));
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
        return apiService.queryRevisionContent(page.getId());
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
}
