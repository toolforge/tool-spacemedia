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
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;

/**
 * Configuration that dynamically creates orgs REST/web controllers.
 */
@Configuration
public class OrgControllerConfiguration {

    @Autowired
    private List<AbstractOrgService<?>> orgs;

    @Autowired
    private AbstractAutowireCapableBeanFactory factory;

    @PostConstruct
    void initOrgControllers() throws ReflectiveOperationException {
        for (Org<?> org : orgs) {
            String orgName = org.getName().replace(" ", "").replace("(", "").replace(")", "");
            registerNewBean(org, orgName + "RestController", RestController.class, OrgRestController.class, "/rest");
            registerNewBean(org, orgName + "WebController", Controller.class, OrgWebController.class, "");
        }
    }

    private void registerNewBean(Org<?> org, String controllerName,
            Class<? extends Annotation> annotationClass, Class<?> parentClass, String pathSuffix)
            throws ReflectiveOperationException {
        Object controller = new ByteBuddy().subclass(parentClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .annotateType(ofType(annotationClass).build(),
                        ofType(RequestMapping.class)
                                .define("path", of(new String[] { org.getId() + pathSuffix })).build())
                .defineConstructor(Visibility.PUBLIC).withParameters(org.getClass())
                .intercept(MethodCall.invoke(parentClass.getDeclaredConstructor(AbstractOrgService.class))
                        .withAllArguments())
                .annotateMethod(ofType(Autowired.class).build()).make().load(getClass().getClassLoader()).getLoaded()
                .getConstructor(org.getClass()).newInstance(org);

        factory.autowireBean(controller);
        factory.initializeBean(controller, controllerName);
        factory.registerSingleton(controllerName, controller);
    }
}
