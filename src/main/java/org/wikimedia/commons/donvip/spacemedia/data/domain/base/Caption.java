package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Embeddable
@Table(indexes = { @Index(columnList = "url") })
public record Caption(

    @Column(nullable = false, length = 8)
    String lang,

    @Column(nullable = false, length = 540)
    URL url) {

}
