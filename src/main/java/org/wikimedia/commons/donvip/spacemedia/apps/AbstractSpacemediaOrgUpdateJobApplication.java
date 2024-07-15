package org.wikimedia.commons.donvip.spacemedia.apps;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.wikimedia.commons.donvip.spacemedia.utils.AnnotationHelper.alterAnnotationOn;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;

@EnableCaching
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(SpacemediaUpdateJobConfiguration.class)
abstract class AbstractSpacemediaOrgUpdateJobApplication implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpacemediaOrgUpdateJobApplication.class);

    @Autowired
    private List<Org<?>> orgs;

    protected static SpringApplicationBuilder app(Class<? extends AbstractSpacemediaOrgUpdateJobApplication> app) {
        return new SpringApplicationBuilder(app).web(WebApplicationType.NONE)
                .listeners((ApplicationEnvironmentPreparedEvent event) -> patchJpaAnnotation(app, event),
                        (ContextRefreshedEvent event) -> registerSimilarOrgs(event));
    }

    private static void patchJpaAnnotation(Class<? extends AbstractSpacemediaOrgUpdateJobApplication> app,
            ApplicationEnvironmentPreparedEvent event) {
        try {
            List<Class<Object>> morePackageClasses = classList(event.getEnvironment(), "similarBasePackageClasses");
            if (morePackageClasses.isEmpty()) {
                return;
            }
            LOGGER.debug("Patching @EnableJpaRepositories annotation to include additional base package classes: {}",
                    morePackageClasses);
            EnableJpaRepositories oldAnnotation = (EnableJpaRepositories) Arrays
                    .stream(app.getAnnotations()).filter(EnableJpaRepositories.class::isInstance).findFirst()
                    .orElseThrow();

            alterAnnotationOn(app, EnableJpaRepositories.class,
                    new EnableJpaRepositories() {

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return oldAnnotation.annotationType();
                        }

                        @Override
                        public Class<?>[] basePackageClasses() {
                            List<Class<?>> basePackageClasses = new ArrayList<>(
                                    Arrays.asList(oldAnnotation.basePackageClasses()));
                            basePackageClasses.addAll(morePackageClasses);
                            return basePackageClasses.toArray(new Class[0]);
                        }

                        @Override
                        public String[] value() {
                            return oldAnnotation.value();
                        }

                        @Override
                        public String[] basePackages() {
                            return oldAnnotation.basePackages();
                        }

                        @Override
                        public Filter[] includeFilters() {
                            return oldAnnotation.includeFilters();
                        }

                        @Override
                        public Filter[] excludeFilters() {
                            return oldAnnotation.excludeFilters();
                        }

                        @Override
                        public String repositoryImplementationPostfix() {
                            return oldAnnotation.repositoryImplementationPostfix();
                        }

                        @Override
                        public String namedQueriesLocation() {
                            return oldAnnotation.namedQueriesLocation();
                        }

                        @Override
                        public Key queryLookupStrategy() {
                            return oldAnnotation.queryLookupStrategy();
                        }

                        @Override
                        public Class<?> repositoryFactoryBeanClass() {
                            return oldAnnotation.repositoryFactoryBeanClass();
                        }

                        @Override
                        public Class<?> repositoryBaseClass() {
                            return oldAnnotation.repositoryBaseClass();
                        }

                        @Override
                        public String entityManagerFactoryRef() {
                            return oldAnnotation.entityManagerFactoryRef();
                        }

                        @Override
                        public String transactionManagerRef() {
                            return oldAnnotation.transactionManagerRef();
                        }

                        @Override
                        public boolean considerNestedRepositories() {
                            return oldAnnotation.considerNestedRepositories();
                        }

                        @Override
                        public boolean enableDefaultTransactions() {
                            return oldAnnotation.enableDefaultTransactions();
                        }

                        @Override
                        public BootstrapMode bootstrapMode() {
                            return oldAnnotation.bootstrapMode();
                        }

                        @Override
                        public char escapeCharacter() {
                            return oldAnnotation.escapeCharacter();
                        }
                    });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void registerSimilarOrgs(ContextRefreshedEvent event) {
        if (event.getApplicationContext() instanceof GenericApplicationContext context) {
            ConfigurableEnvironment env = context.getEnvironment();
            try {
                List<Class<MediaRepository<?>>> similarRepositoryClasses = classList(env, "similarRepositoryClasses");
                @SuppressWarnings("unchecked")
                List<String> similarRepositoryNames = env.getProperty("similarRepositoryNames", List.class);
                for (Class<?> orgClass : classList(env, "similarOrgs")) {
                    if (isEmpty(similarRepositoryClasses) && isEmpty(similarRepositoryNames)) {
                        throw new IllegalStateException(
                                "Missing at least one of 'similarRepositoryClasses' or 'similarRepositoryNames' property.");
                    }
                    if (isEmpty(similarRepositoryClasses)) {
                        for (String repoName : similarRepositoryNames) {
                            try {
                                context.registerBean(orgClass, context.getBean(repoName, MediaRepository.class));
                                LOGGER.debug("Registered bean of type {}", orgClass);
                                break;
                            } catch (BeanDefinitionStoreException e) {
                                LOGGER.trace(e.getMessage());
                            }
                        }
                    } else {
                        for (Class<MediaRepository<?>> repoClass : similarRepositoryClasses) {
                            try {
                                context.registerBean(orgClass, context.getBean(repoClass));
                                LOGGER.debug("Registered bean of type {}", orgClass);
                                break;
                            } catch (BeanDefinitionStoreException e) {
                                LOGGER.trace(e.getMessage());
                            }
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            List<String> activeProfiles = Arrays
                    .asList(event.getApplicationContext().getEnvironment().getActiveProfiles());
            if (!activeProfiles.contains("test")) {
                for (Org<?> org : orgs) {
                    if (org.updateOnProfiles(activeProfiles)) {
                        org.updateMedia(event.getArgs());
                    } else {
                        LOGGER.info("{} does not perform media update with profiles {}", org, activeProfiles);
                    }
                }
            }
        } catch (IOException | UploadException | RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            GlitchTip.capture(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<Class<T>> classList(ConfigurableEnvironment env, String property)
            throws ClassNotFoundException {
        List<Class<T>> result = new ArrayList<>();
        if (env.getProperty(property, List.class) instanceof List<?> classes) {
            for (Object klass : classes) {
                if (klass instanceof String org) {
                    result.add((Class<T>) Class.forName(org));
                }
            }
        }
        return result;
    }
}
