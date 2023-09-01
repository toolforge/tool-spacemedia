package org.wikimedia.commons.donvip.spacemedia.service;

import java.net.URL;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

@FunctionalInterface
public interface UrlResolver<M extends Media> {

    URL resolveDownloadUrl(M media, FileMetadata metadata);
}
