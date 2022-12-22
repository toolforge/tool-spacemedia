package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

class WikidataServiceTest {

    private static final WikibaseDataFetcher fetcher = WikibaseDataFetcher.getWikidataDataFetcher();

    private WikidataService service = new WikidataService();

    @Test
    void testFindCommonsStatementGroup() throws IOException {
        assertEquals(11, service.findCommonsStatementGroup("Category:ISS Expedition 68", "P1029").get().size());
    }

    @Test
    void testFindWikipediaStatementGroup() throws IOException {
        assertEquals(11, service.findWikipediaStatementGroup("Expedition 68", "P1029").get().size());
    }

    @Test
    void testFindFamilyNameByRank() throws Exception {
        assertEquals("Mann", service.findFamilyName((ItemDocument) fetcher.getEntityDocument("Q13500573")));
    }

    @Test
    void testFindFamilyNameByLabel() throws Exception {
        assertEquals("Prokopyev", service.findFamilyName((ItemDocument) fetcher.getEntityDocument("Q21154027")));
    }

    @Test
    void testFindFamilyNameBySingleStatement() throws Exception {
        assertEquals("Kikina", service.findFamilyName((ItemDocument) fetcher.getEntityDocument("Q18352451")));
    }

    @Test
    void testFindCommonsCategory() throws Exception {
        assertEquals("Anna Kikina", service.findCommonsCategory((ItemDocument) fetcher.getEntityDocument("Q18352451")));
    }
}
