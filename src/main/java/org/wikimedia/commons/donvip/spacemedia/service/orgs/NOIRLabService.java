package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;

@Service
public class NOIRLabService extends AbstractOrgDjangoplicityService {

    private static final String BASE_URL = "https://noirlab.edu";

    private static final String PUBLIC_PATH = "/public/";

    private static final String BASE_PUBLIC_URL = BASE_URL + PUBLIC_PATH;

    private static final String IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(BASE_PUBLIC_URL + "([a-z]+/)" + IMAGES_PATH + ".*");

    @Value("${noirlab.date.pattern1}")
    private String datePattern1;

    @Value("${noirlab.date.pattern2}")
    private String datePattern2;

    @Value("${noirlab.datetime.pattern1}")
    private String dateTimePattern1;

    @Value("${noirlab.datetime.pattern2}")
    private String dateTimePattern2;

    @Value("${noirlab.datetime.pattern3}")
    private String dateTimePattern3;

    @Value("${noirlab.datetime.pattern4}")
    private String dateTimePattern4;

    @Value("${noirlab.datetime.pattern5}")
    private String dateTimePattern5;

    @Value("${noirlab.datetime.pattern6}")
    private String dateTimePattern6;

    private DateTimeFormatter dateFormatter1;
    private DateTimeFormatter dateFormatter2;
    private DateTimeFormatter dateTimeFormatter1;
    private DateTimeFormatter dateTimeFormatter2;
    private DateTimeFormatter dateTimeFormatter3;
    private DateTimeFormatter dateTimeFormatter4;
    private DateTimeFormatter dateTimeFormatter5;
    private DateTimeFormatter dateTimeFormatter6;

    @Autowired
    public NOIRLabService(DjangoplicityMediaRepository repository, @Value("${noirlab.search.link}") String searchLink) {
        super(repository, "noirlab", searchLink);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        dateFormatter1 = DateTimeFormatter.ofPattern(datePattern1, Locale.ENGLISH);
        dateFormatter2 = DateTimeFormatter.ofPattern(datePattern2, Locale.ENGLISH);
        // Why CA ? Because only english locale that accepts "a.m." as AM/PM marker
        // https://www.unicode.org/cldr/cldr-aux/charts/33/by_type/date_&_time.gregorian.html#72c7f54616968b69
        dateTimeFormatter1 = DateTimeFormatter.ofPattern(dateTimePattern1, new Locale("en", "CA"));
        dateTimeFormatter2 = DateTimeFormatter.ofPattern(dateTimePattern2, new Locale("en", "CA"));
        dateTimeFormatter3 = DateTimeFormatter.ofPattern(dateTimePattern3, new Locale("en", "CA"));
        dateTimeFormatter4 = DateTimeFormatter.ofPattern(dateTimePattern4, new Locale("en", "CA"));
        dateTimeFormatter5 = DateTimeFormatter.ofPattern(dateTimePattern5, new Locale("en", "CA"));
        dateTimeFormatter6 = DateTimeFormatter.ofPattern(dateTimePattern6, new Locale("en", "CA"));
    }

    @Override
    protected LocalDateTime parseDateTime(String dateTimeText) {
        String text = dateTimeText.replace("Sept.", "Sep.");
        try {
            return super.parseDateTime(text);
        } catch (DateTimeParseException e0) {
            try {
                return LocalDateTime.parse(text, dateTimeFormatter1);
            } catch (DateTimeParseException e1) {
                try {
                    return LocalDateTime.parse(text, dateTimeFormatter2);
                } catch (DateTimeParseException e2) {
                    try {
                        return LocalDateTime.parse(text, dateTimeFormatter3);
                    } catch (DateTimeParseException e3) {
                        try {
                            return LocalDateTime.parse(text, dateTimeFormatter4);
                        } catch (DateTimeParseException e4) {
                            try {
                                return LocalDateTime.parse(text, dateTimeFormatter5);
                            } catch (DateTimeParseException e5) {
                                try {
                                    return LocalDateTime.parse(text, dateTimeFormatter6);
                                } catch (DateTimeParseException e6) {
                                    try {
                                        return LocalDate.parse(text, dateFormatter1).atStartOfDay();
                                    } catch (DateTimeParseException e7) {
                                        return LocalDate.parse(text, dateFormatter2).atStartOfDay();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "NOIRLab";
    }

    @Override
    public Set<String> findLicenceTemplates(DjangoplicityMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("NOIRLab");
        return result;
    }

    @Override
    public URL getSourceUrl(DjangoplicityMedia media, FileMetadata metadata) {
        return newURL(BASE_PUBLIC_URL + IMAGES_PATH + media.getIdUsedInOrg());
    }

    @Override
    protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
        return PATTERN_LOCALIZED_URL.matcher(imgUrlLink);
    }

    @Override
    protected String getCopyrightLink() {
        return "/public/copyright/";
    }

    @Override
    protected Set<String> getTwitterAccounts(DjangoplicityMedia uploadedMedia) {
        return Set.of("@NOIRLabAstro");
    }

    @Override
    protected String mainDivClass() {
        return "col-md-9 left-column";
    }

    @Override
    public Set<String> findCategories(DjangoplicityMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (result.contains("Gemini Observatory")) {
            boolean north = media.containsInTitleOrDescriptionOrKeywords("Gemini North");
            boolean south = media.containsInTitleOrDescriptionOrKeywords("Gemini South");
            if (north && !south) {
                replace(result, "Gemini Observatory", "Gemini North Observatory");
            } else if (south && !north) {
                replace(result, "Gemini Observatory", "Gemini South Observatory");
            }
        }
        return result;
    }
}
