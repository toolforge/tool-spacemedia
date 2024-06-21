package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

public class SpacemediaHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {

    /**
     * Default constructor
     */
    public SpacemediaHttpRequestRetryHandler() {
        this(30, false);
    }

    /**
     * Create the request retry handler using no non-retriable IOException classes.
     *
     * @param retryCount              how many times to retry; 0 means no retries
     * @param requestSentRetryEnabled true if it's OK to retry non-idempotent
     *                                requests that have been sent
     */
    public SpacemediaHttpRequestRetryHandler(final int retryCount, final boolean requestSentRetryEnabled) {
        this(retryCount, requestSentRetryEnabled, List.of());
    }

    /**
     * Create the request retry handler using the specified IOException classes
     *
     * @param retryCount              how many times to retry; 0 means no retries
     * @param requestSentRetryEnabled true if it's OK to retry requests that have
     *                                been sent
     * @param clazzes                 the IOException types that should not be
     *                                retried
     */
    protected SpacemediaHttpRequestRetryHandler(final int retryCount, final boolean requestSentRetryEnabled,
            final Collection<Class<? extends IOException>> clazzes) {
        super(retryCount, requestSentRetryEnabled, clazzes);
    }
}
