package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManagerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.service.commons.CommonsService;

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
    protected Mapper mapper;

    public static final Document html(String path) throws IOException {
        return Jsoup.parse(new File("src/test/resources", path));
    }
}
