package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.articles;

import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

@Entity
@Indexed
public class NasaImageArticleMedia extends Media {

}
