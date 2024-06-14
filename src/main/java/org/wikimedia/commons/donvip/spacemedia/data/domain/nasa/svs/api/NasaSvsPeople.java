package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

public record NasaSvsPeople(
        /** This person's name. */
        String name,
        /** This person's employer, when this page was published. */
        String employer) {

    @Override
    public String toString() {
        return (employer != null ? employer + "/" : "") + name;
    }
}
