package org.wikimedia.commons.donvip.spacemedia.controller;

import static net.bytebuddy.description.annotation.AnnotationDescription.Builder.ofType;
import static net.bytebuddy.description.annotation.AnnotationValue.ForConstant.of;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;

/**
 * Configuration that dynamically creates space agencies REST/web controllers.
 */
@Configuration
public class SpaceAgencyControllerConfiguration {

    @Autowired
    private List<AbstractAgencyService<?, ?, ?, ?, ?, ?>> agencies;

    @Autowired
    private AbstractAutowireCapableBeanFactory factory;

    @PostConstruct
    void initSpaceAgencyControllers() throws ReflectiveOperationException {
        for (Agency<?, ?, ?> agency : agencies) {
            String agencyName = agency.getName().replace(" ", "").replace("(", "").replace(")", "");
            registerNewBean(agency, agencyName + "RestController", RestController.class, SpaceAgencyRestController.class, "/rest");
            registerNewBean(agency, agencyName + "WebController", Controller.class, SpaceAgencyWebController.class, "");
        }
    }

    private void registerNewBean(Agency<?, ?, ?> agency, String controllerName,
            Class<? extends Annotation> annotationClass, Class<?> parentClass, String pathSuffix)
            throws ReflectiveOperationException {
        Object controller = new ByteBuddy().subclass(parentClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .annotateType(ofType(annotationClass).build(),
                        ofType(RequestMapping.class)
                                .define("path", of(new String[] {agency.getId() + pathSuffix})).build())
                .defineConstructor(Visibility.PUBLIC).withParameters(agency.getClass())
                .intercept(MethodCall.invoke(parentClass.getDeclaredConstructor(AbstractAgencyService.class))
                        .withAllArguments())
                .annotateMethod(ofType(Autowired.class).build()).make().load(getClass().getClassLoader()).getLoaded()
                .getConstructor(agency.getClass()).newInstance(agency);

        factory.autowireBean(controller);
        factory.initializeBean(controller, controllerName);
        factory.registerSingleton(controllerName, controller);
    }
}
