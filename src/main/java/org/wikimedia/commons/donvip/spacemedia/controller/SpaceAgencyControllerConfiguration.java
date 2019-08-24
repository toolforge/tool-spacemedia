package org.wikimedia.commons.donvip.spacemedia.controller;

import static net.bytebuddy.description.annotation.AnnotationDescription.Builder.ofType;
import static net.bytebuddy.description.annotation.AnnotationValue.ForConstant.of;

import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;

@Configuration
public class SpaceAgencyControllerConfiguration {

    @Autowired
    private List<AbstractSpaceAgencyService<?, ?>> agencies;

    @Autowired
    private AbstractAutowireCapableBeanFactory factory;

    @PostConstruct
    void initSpaceAgencyControllers() throws ReflectiveOperationException {
        for (AbstractSpaceAgencyService<?, ?> agency : agencies) {
            Class<?> parentClass = SpaceAgencyController.class;
            String controllerName = agency.getName().replace(" ", "").replace("(", "").replace(")", "") + "Controller";
            String path = agency.getClass().getSimpleName().replace("Service", "").toLowerCase(Locale.ENGLISH);

            Object controller = new ByteBuddy()
                    .subclass(parentClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                    .annotateType(
                            ofType(RestController.class).build(),
                            ofType(RequestMapping.class).define("path", of(new String[] {path})).build())
                    .defineConstructor(Visibility.PUBLIC)
                    .withParameters(agency.getClass())
                    .intercept(MethodCall
                            .invoke(parentClass.getDeclaredConstructor(AbstractSpaceAgencyService.class))
                            .withAllArguments())
                    .annotateMethod(ofType(Autowired.class).build())
                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded()
                    .getConstructor(agency.getClass())
                    .newInstance(agency);

            factory.autowireBean(controller);
            factory.initializeBean(controller, controllerName);
            factory.registerSingleton(controllerName, controller);
        }
    }
}
