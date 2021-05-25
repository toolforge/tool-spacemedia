package org.wikimedia.commons.donvip.spacemedia.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wikimedia.commons.donvip.spacemedia.commons.data.CommonsDbConfiguration;
import org.wikimedia.commons.donvip.spacemedia.data.DomainDbConfiguration;

@Configuration
@Import({CommonsDbConfiguration.class, DomainDbConfiguration.class})
public class BackendConfiguration {

}
