package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.time.temporal.Temporal;
import java.util.Objects;

import org.apache.commons.codec.binary.StringUtils;

/**
 * Holding class for original media, duplicated by others, to display similarity score in the UI.
 *
 * @param <ID> the identifier type
 * @param <D>  the media date type
 * @param <M>  the media type
 */
public class DuplicateMedia<ID, D extends Temporal, M extends Media<ID, D>> {

    /**
     * Duplicate information from the database
     */
    private final Duplicate duplicate;

    /**
     * Original media matching the duplicate information
     */
    private final M media;

    /**
     * Constructs a new {@code DuplicateMedia}.
     *
     * @param duplicate Duplicate information from the database
     * @param media Original media matching the duplicate information
     */
    public DuplicateMedia(Duplicate duplicate, M media) {
        this.duplicate = Objects.requireNonNull(duplicate);
        this.media = Objects.requireNonNull(media);
        if (!StringUtils.equals(duplicate.getOriginalId(), media.getId().toString())) {
            throw new IllegalArgumentException("Mismatching duplicate information: " + duplicate + " != " + media);
        }
    }

    /**
     * Returns the duplicate information from the database
     *
     * @return the duplicate information from the database
     */
    public Duplicate getDuplicate() {
        return duplicate;
    }

    /**
     * Returns the original media matching the duplicate information
     *
     * @return the original media matching the duplicate information
     */
    public M getMedia() {
        return media;
    }

    @Override
    public int hashCode() {
        return Objects.hash(duplicate, media);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DuplicateMedia<?, ?, ?> other = (DuplicateMedia<?, ?, ?>) obj;
        return Objects.equals(duplicate, other.duplicate) && Objects.equals(media, other.media);
    }

    @Override
    public String toString() {
        return "DuplicateMedia [duplicate=" + duplicate + ", media=" + media + "]";
    }
}
