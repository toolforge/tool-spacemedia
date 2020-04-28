package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.math.BigInteger;

public interface MetadataProjection {

    /**
     * Returns the perceptual hash value.
     * 
     * @return the perceptual hash value
     */
    BigInteger getPhash();
}
