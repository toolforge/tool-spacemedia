package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "O")
public class Organization extends Person {

}
