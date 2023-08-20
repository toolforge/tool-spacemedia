package org.wikimedia.commons.donvip.spacemedia.data.domain.s3;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class S3MediaIdBridge implements IdentifierBridge<S3MediaId> {

    @Override
    public String toDocumentIdentifier(S3MediaId propertyValue,
            IdentifierBridgeToDocumentIdentifierContext context) {
        return propertyValue.toString();
    }

    @Override
    public S3MediaId fromDocumentIdentifier(String documentIdentifier,
            IdentifierBridgeFromDocumentIdentifierContext context) {
        return new S3MediaId(documentIdentifier);
    }
}
