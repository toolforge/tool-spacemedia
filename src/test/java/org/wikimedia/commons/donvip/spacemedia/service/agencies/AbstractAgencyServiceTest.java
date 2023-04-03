package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManagerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.TestPropertySource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.service.GoogleTranslateService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TwitterService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dozermapper.core.Mapper;

@TestPropertySource("/application-test.properties")
public abstract class AbstractAgencyServiceTest {

    @MockBean
    protected ProblemRepository problemRepository;

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
    protected Mapper mapper;

    public static final Document html(String path) throws IOException {
        return Jsoup.parse(new File("src/test/resources", path));
    }

    @Configuration
    protected static class DefaultAgencyTestConfig {
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
