package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static java.util.stream.Collectors.toMap;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q17444909_ASTRONOMICAL_OBJECT_TYPE;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q3099911_SCIENTIFIC_INSTUMENT;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q4213_TELESCOPE;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q5107_CONTINENT;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q6256_COUNTRY;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q8928_CONSTELLATION;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataProperty.P1448_OFFICIAL_NAME;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataProperty.P1705_NATIVE_LABEL;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataProperty.P373_COMMONS_CATEGORY;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataProperty.P528_CATALOG_CODE;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataProperty.P734_FAMILY_NAME;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.wikibaseapi.WbGetEntitiesSearchData;
import org.wikidata.wdtk.wikibaseapi.WbSearchEntitiesResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

@Service
public class WikidataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikidataService.class);

    private static final WikibaseDataFetcher fetcher = WikibaseDataFetcher.getWikidataDataFetcher();

    private static final SPARQLRepository sparqlRepository = new SPARQLRepository("https://query.wikidata.org/sparql");

    public Optional<Statement> findCommonsStatement(String title, String property) throws IOException {
        return findStatement("commonswiki", title, property);
    }

    public Optional<Statement> findWikipediaStatement(String title, String property) throws IOException {
        return findStatement("enwiki", title, property);
    }

    public Optional<StatementGroup> findCommonsStatementGroup(String title, String property) throws IOException {
        return findStatementGroup("commonswiki", title, property);
    }

    public Optional<StatementGroup> findWikipediaStatementGroup(String title, String property) throws IOException {
        return findStatementGroup("enwiki", title, property);
    }

    Optional<Statement> findStatement(String site, String title, String property) throws IOException {
        return find(site, title, property, ItemDocument::findStatement);
    }

    Optional<StatementGroup> findStatementGroup(String site, String title, String property) throws IOException {
        return find(site, title, property, ItemDocument::findStatementGroup);
    }

    <T> Optional<T> find(String site, String title, String property, BiFunction<ItemDocument, String, T> func)
            throws IOException {
        try {
            if (fetcher.getEntityDocumentByTitle(site, title) instanceof ItemDocument doc) {
                return Optional.ofNullable(func.apply(doc, property));
            }
            return Optional.empty();
        } catch (MediaWikiApiErrorException e) {
            throw new IOException(e);
        }
    }

    public Map<String, String> mapCommonsCategoriesByFamilyName(Collection<Statement> statements) {
        return statements.parallelStream().map(s -> {
            try {
                if (s.getValue() instanceof EntityIdValue val
                        && fetcher.getEntityDocument(val.getId()) instanceof ItemDocument doc) {
                    return doc;
                }
            } catch (MediaWikiApiErrorException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            return null;
        }).filter(Objects::nonNull).collect(toMap(this::findFamilyName, this::findCommonsCategory));
    }

    public String findCommonsCategory(ItemDocument doc) {
        Statement statement = doc.findStatement(P373_COMMONS_CATEGORY.toString());
        return statement != null && statement.getValue() instanceof StringValue sv ? sv.getString() : null;
    }

    public String findFamilyName(ItemDocument doc) {
        Statement statement = doc.findStatement(P734_FAMILY_NAME.toString());
        if (statement != null) {
            return findEnglishLabel(statement);
        }
        StatementGroup statementGroup = doc.findStatementGroup(P734_FAMILY_NAME.toString());
        if (statementGroup != null) {
            StatementGroup best = statementGroup.getBestStatements();
            return findEnglishLabel((best != null ? best : statementGroup).getStatements().get(0));
        }
        String enLabel = findEnglishLabel(doc);
        if (enLabel != null) {
            String[] words = enLabel.split(" ");
            if (words.length > 1) {
                return words[words.length - 1];
            }
            return enLabel;
        }
        return null;
    }

    public String findEnglishLabel(Statement st) {
        try {
            if (st.getValue() instanceof EntityIdValue eiv
                    && fetcher.getEntityDocument(eiv.getId()) instanceof ItemDocument doc) {
                return findEnglishLabel(doc);
            }
        } catch (MediaWikiApiErrorException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public String findEnglishLabel(ItemDocument doc) {
        MonolingualTextValue enLabel = doc.getLabels().get("en");
        return enLabel != null ? enLabel.getText() : null;
    }

    @Cacheable("wikidataSearchedEntities")
    public List<WbSearchEntitiesResult> searchEntities(String search) throws IOException {
        try {
            WbGetEntitiesSearchData properties = new WbGetEntitiesSearchData();
            properties.search = search;
            properties.language = "en";
            properties.type = "item";
            properties.limit = 500L;
            return fetcher.searchEntities(properties);
        } catch (MediaWikiApiErrorException e) {
            throw new IOException(e);
        }
    }

    @Cacheable("wikidataContinents")
    public Optional<Pair<String, String>> searchContinent(String name) {
        return searchCommonsCategory(WikidataService::getContinentQuery, name);
    }

    @Cacheable("wikidataCountries")
    public Optional<Pair<String, String>> searchCountry(String name) {
        return searchCommonsCategory(WikidataService::getCountryQuery, name);
    }

    @Cacheable("wikidataAstronomicalObjects")
    public Optional<Pair<String, String>> searchAstronomicalObject(String catalogName) {
        return searchCommonsCategory(WikidataService::getAstronomicalObjectQuery, catalogName);
    }

    @Cacheable("wikidataConstellations")
    public Optional<Pair<String, String>> searchConstellation(String officialName) {
        return searchCommonsCategory(WikidataService::getConstellationQuery, officialName);
    }

    @Cacheable("wikidataTelescopes")
    public Optional<Pair<String, String>> searchTelescope(String name) {
        return searchCommonsCategory(WikidataService::getTelescopeQuery, name);
    }

    @Cacheable("wikidataInstruments")
    public Optional<Pair<String, String>> searchInstrument(String name) {
        return searchCommonsCategory(WikidataService::getInstrumentQuery, name);
    }

    private static Optional<Pair<String, String>> searchCommonsCategory(UnaryOperator<String> query, String name) {
        try (RepositoryConnection sparqlConnection = sparqlRepository.getConnection();
                TupleQueryResult result = sparqlConnection
                        .prepareTupleQuery(QueryLanguage.SPARQL, query.apply(name)).evaluate()) {
            List<BindingSet> results = result.stream().toList();
            if (results.size() != 1) {
                LOGGER.info("Not exactly 1 item when looking for {} in Wikidata: {}", name, results);
            } else {
                BindingSet firstResult = results.get(0);
                String itemId = firstResult.getBinding("item").getValue().stringValue()
                        .replace("http://www.wikidata.org/entity/", "");
                Binding commonsCat = firstResult.getBinding("commonsCat");
                return Optional.of(Pair.of(itemId, commonsCat != null ? commonsCat.getValue().stringValue() : null));
            }
        }
        return Optional.empty();
    }


    private static String getContinentQuery(String name) {
        return getNamedObjectQuery(Q5107_CONTINENT, P1705_NATIVE_LABEL, name, "en");
    }

    private static String getCountryQuery(String name) {
        return getNamedObjectQuery(Q6256_COUNTRY, P1705_NATIVE_LABEL, name, "en");
    }

    private static String getAstronomicalObjectQuery(String catalogName) {
        return getNamedObjectQuery(Q17444909_ASTRONOMICAL_OBJECT_TYPE, P528_CATALOG_CODE, catalogName);
    }

    private static String getConstellationQuery(String officialName) {
        return getNamedObjectQuery(Q8928_CONSTELLATION, P1448_OFFICIAL_NAME, officialName, "la");
    }

    private static String getTelescopeQuery(String name) {
        return getNamedObjectQuery(Q4213_TELESCOPE, P1705_NATIVE_LABEL, name, "en");
    }

    private static String getInstrumentQuery(String name) {
        return getNamedObjectQuery(Q3099911_SCIENTIFIC_INSTUMENT, P1705_NATIVE_LABEL, name, "en");
    }

    private static String getNamedObjectQuery(WikidataItem natureQid, WikidataProperty namingProperty, String name) {
        return """
                SELECT DISTINCT ?item ?itemLabel ?commonsCat
                WHERE
                {
                  ?item wdt:P31/wdt:P31/wdt:P279* wd:$natureQid;
                        wdt:$namingProperty "$name".
                  OPTIONAL { ?item wdt:P373 ?commonsCat }.
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE]". }
                }
                LIMIT 2
                """.replace("$name", name.replace("\"", "")).replace("$namingProperty", namingProperty.toString())
                .replace("$natureQid", natureQid.toString());
    }

    private static String getNamedObjectQuery(WikidataItem natureQid, WikidataProperty namingProperty, String name,
            String lang) {
        return """
                SELECT DISTINCT ?item ?itemLabel ?commonsCat
                WHERE
                {
                  ?item wdt:P31/wdt:P279* wd:$natureQid;
                        wdt:$namingProperty ?name FILTER(?name = "$name"@$lang).
                  OPTIONAL { ?item wdt:P373 ?commonsCat }.
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE]". }
                }
                LIMIT 2
                """.replace("$lang", lang).replace("$name", name.replace("\"", ""))
                .replace("$namingProperty", namingProperty.toString()).replace("$natureQid", natureQid.toString());
    }
}
