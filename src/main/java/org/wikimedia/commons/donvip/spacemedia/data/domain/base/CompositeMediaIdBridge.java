package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class CompositeMediaIdBridge implements IdentifierBridge<CompositeMediaId> {

    @Override
    public String toDocumentIdentifier(CompositeMediaId propertyValue,
            IdentifierBridgeToDocumentIdentifierContext context) {
        return propertyValue.toString();
    }

    @Override
    public CompositeMediaId fromDocumentIdentifier(String documentIdentifier,
            IdentifierBridgeFromDocumentIdentifierContext context) {
        return new CompositeMediaId(documentIdentifier);
    }
}
