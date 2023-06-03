package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class DjangoplicityMediaIdBridge implements IdentifierBridge<DjangoplicityMediaId> {

    @Override
    public String toDocumentIdentifier(DjangoplicityMediaId propertyValue,
            IdentifierBridgeToDocumentIdentifierContext context) {
        return propertyValue.toString();
    }

    @Override
    public DjangoplicityMediaId fromDocumentIdentifier(String documentIdentifier,
            IdentifierBridgeFromDocumentIdentifierContext context) {
        return new DjangoplicityMediaId(documentIdentifier);
    }
}
