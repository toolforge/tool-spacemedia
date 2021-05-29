package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

public class DvidsMediaTypedIdFieldBridge implements TwoWayFieldBridge {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        luceneOptions.addFieldToDocument(name, value.toString(), document);
    }

    @Override
    public Object get(String name, Document document) {
        return new DvidsMediaTypedId(name);
    }

    @Override
    public String objectToString(Object object) {
        return object.toString();
    }
}
