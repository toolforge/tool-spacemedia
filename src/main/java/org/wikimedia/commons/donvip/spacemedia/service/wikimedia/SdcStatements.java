package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

public class SdcStatements extends TreeMap<String, Pair<Object, Map<String, Object>>> {

    private static final long serialVersionUID = 1L;

    public SdcStatements capturedWith(String qid) {
        return addStatement(WikidataProperty.P4082_CAPTURED_WITH, qid);
    }

    public SdcStatements constellation(String qid) {
        return addStatement(WikidataProperty.P59_CONSTELLATION, qid);
    }

    public SdcStatements creator(String qid) {
        return addStatement(WikidataProperty.P170_CREATOR, qid);
    }

    public SdcStatements creator(String text, Pattern pattern, Map<String, String> map) {
        Matcher m = pattern.matcher(text);
        return m.matches() ? creator(map.get(m.group(1))) : this;
    }

    public SdcStatements depicts(String qid) {
        return addStatement(WikidataProperty.P180_DEPICTS, qid);
    }

    public SdcStatements fabricationMethod(String qid) {
        return addStatement(WikidataProperty.P2079_FABRICATION_METHOD, qid);
    }

    public SdcStatements instanceOf(WikidataItem item) {
        return addStatement(WikidataProperty.P31_INSTANCE_OF, item);
    }

    public SdcStatements locationOfCreation(String qid) {
        return addStatement(WikidataProperty.P1071_LOCATION_OF_CREATION, qid);
    }

    private SdcStatements addStatement(WikidataProperty prop, Object value) {
        put(prop.toString(), Pair.of(value, null));
        return this;
    }
}
