package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

class WikidataServiceTest {

    private static final WikibaseDataFetcher fetcher = WikibaseDataFetcher.getWikidataDataFetcher();

    private WikidataService service = new WikidataService();

    @Test
    void testFindCommonsStatementGroup() throws IOException {
        assertEquals(15, service.findCommonsStatementGroup("Category:ISS Expedition 68", "P1029").get().size());
    }

    @Test
    void testFindWikipediaStatementGroup() throws IOException {
        assertEquals(15, service.findWikipediaStatementGroup("Expedition 68", "P1029").get().size());
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

    @Test
    void testMapCommonsCategoriesByFamilyName() throws IOException {
        assertEquals(
                Map.ofEntries(e("Mann", "Nicole Mann"), e("Cassada", "Josh A. Cassada"), e("Hines", "Robert Hines"),
                        e("Wakata", "Koichi Wakata"), e("Watkins", "Jessica Watkins"), e("Petelin", "Dmitri Petelin"),
                        e("Prokopyev", "Sergey Prokopyev (cosmonaut)"), e("Cristoforetti", "Samantha Cristoforetti"),
                        e("Lindgren", "Kjell Lindgren"), e("Kikina", "Anna Kikina"), e("Rubio", "Francisco Rubio"),
                        e("Bowen", "Stephen G. Bowen"), e("Hoburg", "Warren Hoburg"), e("Niadi", "Sultan Al Neyadi"),
                        e("Fedyayev", "Andrey Fedyaev")),
                service.mapCommonsCategoriesByFamilyName(
                        service.findCommonsStatementGroup("Category:ISS Expedition 68", "P1029").get()));
    }

    @Test
    void testSearchAstronomicalObject() {
        assertEquals(Optional.of(Pair.of("Q86709121", null)), service.searchAstronomicalObject("[KAG2008] globule 13"));
    }

    private static final SimpleEntry<String, String> e(String k, String v) {
        return new SimpleEntry<>(k, v);
    }
}
