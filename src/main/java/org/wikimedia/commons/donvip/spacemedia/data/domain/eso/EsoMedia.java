package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
@Table(indexes = {@Index(columnList = "sha1,full_res_sha1")})
public class EsoMedia extends CommonEsoMedia {

}
