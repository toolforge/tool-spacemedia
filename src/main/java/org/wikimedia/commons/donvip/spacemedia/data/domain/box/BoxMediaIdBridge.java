package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class BoxMediaIdBridge implements IdentifierBridge<BoxMediaId> {

    @Override
    public String toDocumentIdentifier(BoxMediaId propertyValue,
            IdentifierBridgeToDocumentIdentifierContext context) {
        return propertyValue.toString();
    }

    @Override
    public BoxMediaId fromDocumentIdentifier(String documentIdentifier,
            IdentifierBridgeFromDocumentIdentifierContext context) {
        return new BoxMediaId(documentIdentifier);
    }
}
