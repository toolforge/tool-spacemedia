package org.wikimedia.commons.donvip.spacemedia.service.wikimedia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sentry.Sentry;

public final class GlitchTip {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlitchTip.class);

    private GlitchTip() {
        // Hide default constructore
    }

    static {
        // Use the DSN detection system (Java System Property "sentry.dsn")
        try {
            Sentry.init();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to initialize Sentry client. GlitchTip error tracking will not work: {}",
                    e.getMessage());
        }
    }

    public static void capture(Throwable t) {
        // This sends an exception event to Sentry using the statically stored instance
        // that was created in the static block
        try {
            Sentry.captureException(t);
        } catch (RuntimeException ignored) {
            LOGGER.trace("Failed to capture exception with GlitchTip: {}", ignored.getMessage());
        }
    }

    public static void setTag(String key, String value) {
        try {
            Sentry.setTag(key, value);
        } catch (RuntimeException ignored) {
            LOGGER.trace("Failed to set tag with GlitchTip: {}", ignored.getMessage());
        }
    }

    public static void setExtra(String key, String value) {
        try {
            Sentry.setExtra(key, value);
        } catch (RuntimeException ignored) {
            LOGGER.trace("Failed to set extra with GlitchTip: {}", ignored.getMessage());
        }
    }

    public static void removeTag(String... keys) {
        try {
            for (String key : keys) {
                Sentry.removeTag(key);
            }
        } catch (RuntimeException ignored) {
            LOGGER.trace("Failed to remove tag with GlitchTip: {}", ignored.getMessage());
        }
    }

    public static void removeExtra(String... keys) {
        try {
            for (String key : keys) {
                Sentry.removeExtra(key);
            }
        } catch (RuntimeException ignored) {
            LOGGER.trace("Failed to remove extra with GlitchTip: {}", ignored.getMessage());
        }
    }
}
