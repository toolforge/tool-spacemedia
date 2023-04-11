package org.wikimedia.commons.donvip.spacemedia.data.domain.esa.hubble;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;

@Entity
@Table(indexes = { @Index(columnList = "sha1, full_res_sha1, phash, full_res_phash") })
public class HubbleEsaMedia extends DjangoplicityMedia {

}
