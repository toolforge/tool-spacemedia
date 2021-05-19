package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

public class DvidsMediaTypeIdResolver extends TypeIdResolverBase {

    private static final Map<String, Class<? extends DvidsMedia>> classes = Map.of(
            "audio", DvidsAudio.class,
            "image", DvidsImage.class,
            "graphic", DvidsGraphic.class,
            "news", DvidsNews.class,
            "publication_issue", DvidsPublication.class,
            "video", DvidsVideo.class,
            "webcast", DvidsWebcast.class
            );
    
    private JavaType superType;

    @Override
    public void init(JavaType baseType) {
        superType = baseType;
    }

    @Override
    public Id getMechanism() {
        return Id.NAME;
    }

    @Override
    public String idFromValue(Object obj) {
        return idFromValueAndType(obj, obj.getClass());
    }

    @Override
    public String idFromValueAndType(Object obj, Class<?> subType) {
        return classes.entrySet().stream().filter(e -> e.getValue().equals(subType)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(subType.toString())).getKey();
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        return context.constructSpecializedType(superType, classes.get(id.split(":")[0]));
    }
}
