package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

class DjangoplicityMediaTest {

    @Test
    void testCopyFrom() {
        DjangoplicityMedia m1 = new DjangoplicityMedia();
        m1.setCategories(Set.of("test"));
        m1.setConstellation("Dorado");
        m1.setCredit("foo");
        m1.setDistance("bar");
        m1.setFieldOfView("baz");
        m1.setImageType(DjangoplicityMediaType.Artwork);
        m1.setInstruments(Set.of("wololo"));
        m1.setLicence(DjangoplicityLicence.CC_BY_4_0);
        m1.setName("foobar");
        m1.setOrientation("left");
        m1.setPositionDec("0");
        m1.setPositionRa("1");
        m1.setRelatedAnnouncements(Set.of("news"));
        m1.setRelatedReleases(Set.of("rlz"));
        m1.setTelescopes(Set.of("Hubble"));
        m1.setTypes(Set.of("foobaz"));

        DjangoplicityMedia m2 = new DjangoplicityMedia();
        m2.copyDataFrom(m1);

        assertEquals(Set.of("test"), m2.getCategories());
        assertEquals("Dorado", m2.getConstellation());
        assertEquals("foo", m2.getCredit());
        assertEquals("bar", m2.getDistance());
        assertEquals("baz", m2.getFieldOfView());
        assertEquals(DjangoplicityMediaType.Artwork, m2.getImageType());
        assertEquals(Set.of("wololo"), m2.getInstruments());
        assertEquals(DjangoplicityLicence.CC_BY_4_0, m2.getLicence());
        assertEquals("foobar", m2.getName());
        assertEquals("left", m2.getOrientation());
        assertEquals("0", m2.getPositionDec());
        assertEquals("1", m2.getPositionRa());
        assertEquals(Set.of("news"), m2.getRelatedAnnouncements());
        assertEquals(Set.of("rlz"), m2.getRelatedReleases());
        assertEquals(Set.of("Hubble"), m2.getTelescopes());
        assertEquals(Set.of("foobaz"), m2.getTypes());
    }
}
