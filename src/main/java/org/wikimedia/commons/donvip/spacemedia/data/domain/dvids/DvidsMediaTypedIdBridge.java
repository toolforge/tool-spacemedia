package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class DvidsMediaTypedIdBridge implements IdentifierBridge<DvidsMediaTypedId> {

    @Override
    public String toDocumentIdentifier(DvidsMediaTypedId propertyValue,
            IdentifierBridgeToDocumentIdentifierContext context) {
        return propertyValue.toString();
    }

    @Override
    public DvidsMediaTypedId fromDocumentIdentifier(String documentIdentifier,
            IdentifierBridgeFromDocumentIdentifierContext context) {
        return new DvidsMediaTypedId(documentIdentifier);
    }
}
