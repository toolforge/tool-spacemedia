package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "P")
public class PdfFile extends File {

}
