package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.Arrays.stream;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;

@Lazy
@Service
public class CategorizationService {

    static final Pattern COPERNICUS_CREDIT = Pattern.compile(
            ".*Copernicus[ -](?:Sentinel[ -])?dat(?:a|en)(?:/ESA)? [\\(\\[](2\\d{3}(?:[-–/]\\d{2,4})?)[\\)\\]].*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SENTINEL_SAT = Pattern.compile(".*Sentinel[ -]?[1-6].*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Logger LOGGER = LoggerFactory.getLogger(CategorizationService.class);

    private Set<String> satellitePicturesCategories;

    private Map<String, String> categoriesStatements;

    @Lazy
    @Autowired
    protected CommonsService commonsService;

    @PostConstruct
    void init() throws IOException {
        satellitePicturesCategories = CsvHelper.loadSet(getClass().getResource("/satellite.pictures.categories.txt"));
        categoriesStatements = CsvHelper.loadCsvMapping("categories.statements.csv");
    }

    public static String extractCopernicusTemplate(String text) {
        Matcher m = COPERNICUS_CREDIT.matcher(text);
        return m.matches() ? getCopernicusTemplate(m.group(1)) : null;
    }

    public static String getCopernicusTemplate(String year) {
        return "Attribution-Copernicus |year=" + year;
    }

    public static String getCopernicusTemplate(int year) {
        return getCopernicusTemplate(Integer.toString(year));
    }

    public void findCategoriesStatements(SdcStatements result, Set<String> cats) {
        for (Entry<String, String> e : categoriesStatements.entrySet()) {
            if (stream(e.getKey().split(";")).map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE))
                    .anyMatch(p -> cats.stream().anyMatch(c -> p.matcher(c).matches()))) {
                LOGGER.info("SDC category match: {}", e);
                for (String statement : e.getValue().split(";")) {
                    String[] kv = statement.split("=");
                    Pair<Object, Map<String, Object>> old = result.put(kv[0], Pair.of(kv[1], null));
                    if (old != null) {
                        LOGGER.warn("Replaced old SDC: {}", old);
                    }
                }
            }
        }
    }

    public boolean isFromSentinelSatellite(Media media) {
        return SENTINEL_SAT.matcher(media.getTitle()).matches()
                || (media.getDescription() != null && SENTINEL_SAT.matcher(media.getDescription()).matches())
                || (media instanceof WithKeywords mkw
                        && mkw.getKeywordStream().anyMatch(kw -> SENTINEL_SAT.matcher(kw).matches()));
    }

    public void findCategoriesForSentinels(Media media, Set<String> result) {
        if (isFromSentinelSatellite(media)) {
            result.add(getCopernicusTemplate(media.getYear().getValue()));
            if (media.containsInTitleOrDescriptionOrKeywords("fires", "burn scars", "wildfire", "forest fire")) {
                result.add("Photos of wildfires by Sentinel satellites");
            } else if (media.containsInTitleOrDescriptionOrKeywords("Phytoplankton", "algal bloom")) {
                result.add("Satellite pictures of algal blooms");
            } else if (media.containsInTitleOrDescriptionOrKeywords("hurricane")) {
                result.add("Satellite pictures of hurricanes");
            } else if (media.containsInTitleOrDescriptionOrKeywords("floods", "flooding")) {
                result.add("Photos of floods by Sentinel satellites");
            }
        }
        for (String num : new String[] { "1", "2", "3", "4", "5", "5P", "6" }) {
            findCategoriesForSentinel(media, "Sentinel-" + num, result);
        }
    }

    private void findCategoriesForSentinel(Media media, String sentinel, Set<String> result) {
        if (media.containsInTitleOrDescriptionOrKeywords(sentinel)) {
            result.addAll(findCategoriesForEarthObservationImage(media, x -> "Photos of " + x + " by " + sentinel,
                    sentinel + " images", true, true, true));
        }
    }

    public Set<String> findCategoriesForEarthObservationImage(Media image, UnaryOperator<String> categorizer,
            String defaultCat, boolean lookIntoTitle, boolean lookIntoDescription, boolean lookIntoKeywords) {
        Set<String> result = new TreeSet<>();
        for (String targetOrSubject : satellitePicturesCategories) {
            if (image.containsInTitleOrDescriptionOrKeywords(targetOrSubject, lookIntoTitle, lookIntoDescription,
                    lookIntoKeywords)) {
                findCategoryForEarthObservationTargetOrSubject(categorizer, targetOrSubject).ifPresent(result::add);
            }
        }
        if (result.isEmpty()) {
            result.add(defaultCat);
        }
        return result;
    }

    public Optional<String> findCategoryForEarthObservationTargetOrSubject(UnaryOperator<String> categorizer,
            String targetOrSubject) {
        String cat = categorizer.apply(targetOrSubject);
        if (commonsService.existsCategoryPage(cat)) {
            return Optional.of(cat);
        } else {
            String theCat = categorizer.apply("the " + targetOrSubject);
            if (commonsService.existsCategoryPage(theCat)) {
                return Optional.of(theCat);
            } else {
                String cats = categorizer.apply(targetOrSubject + "s");
                if (commonsService.existsCategoryPage(cats)) {
                    return Optional.of(cats);
                }
            }
        }
        return Optional.empty();
    }
}
