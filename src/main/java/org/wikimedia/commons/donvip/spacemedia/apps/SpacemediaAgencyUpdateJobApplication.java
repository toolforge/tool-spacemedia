package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;

public class SpacemediaAgencyUpdateJobApplication extends AbstractSpacemediaAgencyUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaAgencyUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Agency<?, ?, ?> agency(@Value("${agency}") String agency, @Value("${repository}") String repository,
            ApplicationContext context) throws ReflectiveOperationException {
        Class<?> repoClass = Class.forName(repository);
        return (Agency<?, ?, ?>) Class.forName(agency).getConstructor(repoClass)
                .newInstance((MediaRepository<?, ?, ?>) context.getBean(repoClass));
    }
}