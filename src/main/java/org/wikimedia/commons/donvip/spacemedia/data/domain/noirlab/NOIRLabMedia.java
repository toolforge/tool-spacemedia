package org.wikimedia.commons.donvip.spacemedia.data.domain.noirlab;

import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;

@Entity
@Indexed
public class NOIRLabMedia extends DjangoplicityMedia {

}
