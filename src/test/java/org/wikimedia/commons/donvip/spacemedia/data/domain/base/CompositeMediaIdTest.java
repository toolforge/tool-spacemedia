package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompositeMediaIdTest {

    @Test
    void testConstructor() {
        CompositeMediaId id = new CompositeMediaId("foo:bar");
        assertEquals("foo", id.getRepoId());
        assertEquals("bar", id.getMediaId());

        id = new CompositeMediaId("foo:bar:baz");
        assertEquals("foo", id.getRepoId());
        assertEquals("bar:baz", id.getMediaId());
    }
}
