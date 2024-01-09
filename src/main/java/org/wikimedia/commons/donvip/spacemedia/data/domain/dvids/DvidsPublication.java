package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.Entity;

@Entity
@Indexed
public class DvidsPublication extends DvidsMedia {
}
