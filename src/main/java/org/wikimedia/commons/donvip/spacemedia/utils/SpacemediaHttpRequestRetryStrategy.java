package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.TimeValue;

public class SpacemediaHttpRequestRetryStrategy extends DefaultHttpRequestRetryStrategy {

    /**
     * Default constructor
     */
    public SpacemediaHttpRequestRetryStrategy() {
        this(30, TimeValue.ofSeconds(1L));
    }

    /**
     * Create the request retry handler using no non-retriable IOException classes.
     *
     * @param maxRetries           how many times to retry; 0 means no retries
     * @param defaultRetryInterval the default retry interval between subsequent
     *                             retries if the {@code Retry-After} header is not
     *                             set or invalid.
     */
    public SpacemediaHttpRequestRetryStrategy(final int maxRetries, final TimeValue defaultRetryInterval) {
        this(maxRetries, defaultRetryInterval, List.of(),
                List.of(HttpStatus.SC_TOO_MANY_REQUESTS, HttpStatus.SC_SERVICE_UNAVAILABLE));
    }

    /**
     * Create the request retry handler using the specified IOException classes
     *
     * @param maxRetries           how many times to retry; 0 means no retries
     * @param defaultRetryInterval the default retry interval between subsequent
     *                             retries if the {@code Retry-After} header is not
     *                             set or invalid.
     * @param clazzes              the IOException types that should not be retried
     * @param codes                HTTP status codes which shall be retried
     */
    protected SpacemediaHttpRequestRetryStrategy(final int maxRetries, final TimeValue defaultRetryInterval,
            final Collection<Class<? extends IOException>> clazzes, final Collection<Integer> codes) {
        super(maxRetries, defaultRetryInterval, clazzes, codes);
    }
}
