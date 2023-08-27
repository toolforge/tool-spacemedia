package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManagerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.TypedArgumentConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.TestPropertySource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ExifMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaType;
import org.wikimedia.commons.donvip.spacemedia.service.GoogleTranslateService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TwitterService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataService;

import com.fasterxml.jackson.databind.ObjectMapper;

@TestPropertySource("/application-test.properties")
public abstract class AbstractOrgServiceTest {

    @MockBean
    protected FileMetadataRepository metadataRepository;

    @MockBean
    protected ProblemRepository problemRepository;

    @MockBean
    protected ExifMetadataRepository exifRepository;

    @MockBean
    protected RuntimeDataRepository runtimeDataRepository;

    @MockBean(name = "domain")
    protected EntityManagerFactory entityManager;

    @MockBean
    protected CommonsService commonsService;

    @MockBean
    protected MediaService mediaService;

    @MockBean
    protected RemoteService remoteService;

    @MockBean
    protected SearchService searchService;

    @MockBean
    protected TransactionService transactionService;

    @MockBean
    protected GoogleTranslateService translate;

    @MockBean
    protected TwitterService twitter;

    @MockBean
    protected WikidataService wikidata;

    @Autowired
    protected ObjectMapper jackson;

    public static final Document html(String path) throws IOException {
        return Jsoup.parse(new File("src/test/resources", path));
    }

    public final <T> T json(String path, Class<T> resultClass) throws IOException {
        return jackson.readValue(new File("src/test/resources", path), resultClass);
    }

    protected static void doDjangoplicityMediaTest(DjangoplicityMedia media, String id,
            DjangoplicityMediaType imageType, String date, ImageDimensions dimensions, String name, Set<String> types,
            Set<String> categories, String credit, List<String> assetUrls, String title, String description,
            Set<String> telescopes) {
        assertEquals(id, media.getId().getMediaId());
        assertEquals(imageType, media.getImageType());
        assertEquals(date, media.getPublicationDateTime().toLocalDateTime().toString());
        assertEquals(dimensions, media.getMetadata().iterator().next().getImageDimensions());
        assertEquals(name, media.getName());
        assertEquals(types, media.getTypes());
        assertEquals(categories, media.getCategories());
        assertEquals(credit, media.getCredit());
        assertEquals(assetUrls, media.getMetadata().stream().map(m -> m.getAssetUrl().toExternalForm()).toList());
        assertEquals(title, media.getTitle());
        if (description != null) {
            assertEquals(description, media.getDescription());
        }
        assertEquals(telescopes, media.getTelescopes());
    }

    @SuppressWarnings("rawtypes")
    protected static class ListArgumentConverter extends TypedArgumentConverter<String, List> {

        protected ListArgumentConverter() {
            super(String.class, List.class);
        }

        @Override
        protected List<String> convert(String source) throws ArgumentConversionException {
            return source != null ? Arrays.stream(source.split(",")).map(String::trim).toList() : List.of();
        }
    }

    @SuppressWarnings("rawtypes")
    protected static class SetArgumentConverter extends TypedArgumentConverter<String, Set> {

        protected SetArgumentConverter() {
            super(String.class, Set.class);
        }

        @Override
        protected Set<String> convert(String source) throws ArgumentConversionException {
            return source != null ? new TreeSet<>(Arrays.stream(source.split(",")).map(String::trim).toList())
                    : Set.of();
        }
    }

    @Configuration
    protected static class DefaultOrgTestConfig {
        @Bean
        public ConversionService conversionService() {
            return ApplicationConversionService.getSharedInstance();
        }

        @Bean
        public ObjectMapper jackson() {
            return new ObjectMapper();
        }
    }
}
