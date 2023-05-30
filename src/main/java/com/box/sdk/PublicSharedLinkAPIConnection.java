package com.box.sdk;

public class PublicSharedLinkAPIConnection extends SharedLinkAPIConnection {

    public PublicSharedLinkAPIConnection(BoxAPIConnection connection, String sharedLink) {
        super(connection, sharedLink);
    }
}