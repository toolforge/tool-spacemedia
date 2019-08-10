package org.wikimedia.commons.donvip.spacemedia.data.local.flickr;

import com.flickr4java.flickr.tags.Tag;
import com.github.dozermapper.core.DozerConverter;

public class FlickrTagToStringConverter extends DozerConverter<Tag, String> {

    public FlickrTagToStringConverter() {
        super(Tag.class, String.class);
    }

    @Override
    public String convertTo(Tag source, String destination) {
        return source.getValue();
    }

    @Override
    public Tag convertFrom(String source, Tag destination) {
        throw new UnsupportedOperationException();
    }
}
