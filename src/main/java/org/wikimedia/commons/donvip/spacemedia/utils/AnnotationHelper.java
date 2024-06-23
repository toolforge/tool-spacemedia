package org.wikimedia.commons.donvip.spacemedia.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class AnnotationHelper {

    private AnnotationHelper() {

    }

    @SuppressWarnings("unchecked")
    public static Annotation alterAnnotationOn(Class<?> classToLookFor, Class<? extends Annotation> annotationToAlter,
            Annotation annotationValue)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Method method = Class.class.getDeclaredMethod("annotationData", (Class<?>[]) null);
        method.setAccessible(true); // NOSONAR
        Object annotationData = method.invoke(classToLookFor);
        Field annotations = annotationData.getClass().getDeclaredField("annotations");
        annotations.setAccessible(true); // NOSONAR
        return ((Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationData)).put(annotationToAlter,
                annotationValue);
    }
}
