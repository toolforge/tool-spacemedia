package org.wikimedia.commons.donvip.spacemedia.data.domain.iau;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMedia;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, full_res_sha1, phash, full_res_phash") })
public class IauMedia extends CommonEsoMedia {

}
