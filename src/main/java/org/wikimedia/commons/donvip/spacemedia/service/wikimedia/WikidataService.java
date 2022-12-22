package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

@Service
public class WikidataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikidataService.class);

    private static final WikibaseDataFetcher fetcher = WikibaseDataFetcher.getWikidataDataFetcher();

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

    public Map<String, String> mapCommonsCategoriesByName(Collection<Statement> statements) {
        return statements.stream().map(s -> {
            try {
                if (fetcher.getEntityDocument(s.getSubject().getId()) instanceof ItemDocument doc) {
                    return doc;
                }
            } catch (MediaWikiApiErrorException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            return null;
        }).filter(Objects::nonNull).collect(toMap(this::findFamilyName, this::findCommonsCategory));
    }

    public String findCommonsCategory(ItemDocument doc) {
        Statement statement = doc.findStatement("P373");
        return statement != null && statement.getValue() instanceof StringValue sv ? sv.getString() : null;
    }

    public String findFamilyName(ItemDocument doc) {
        Statement statement = doc.findStatement("P734");
        if (statement != null) {
            return findEnglishLabel(statement);
        }
        StatementGroup statementGroup = doc.findStatementGroup("P734");
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
}
