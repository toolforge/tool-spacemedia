package org.wikimedia.commons.donvip.spacemedia.apps;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;

public class SpacemediaAgencyEsoUpdateJobApplication extends AbstractSpacemediaAgencyUpdateJobApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpacemediaAgencyEsoUpdateJobApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public Agency<?, ?, ?> agency(@Value("${agency}") String agency, @Value("${search.link}") String searchLink,
            @Value("${repository}") String repository, ApplicationContext context) throws ReflectiveOperationException {
        Class<?> repoClass = Class.forName(repository);
        return (Agency<?, ?, ?>) Class.forName(agency).getConstructor(repoClass, String.class)
                .newInstance(context.getBean(repoClass), searchLink);
    }
}
