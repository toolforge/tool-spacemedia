package org.wikimedia.commons.donvip.spacemedia.data.jpa.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MetadataKey;

public interface MetadataRepository extends CrudRepository<Metadata, MetadataKey> {

    default Metadata findOrCreate(String context, String key, String value) {
        MetadataKey mkey = MetadataKey.from(context, key, value);
        Optional<Metadata> opt = findById(mkey);
        return opt.isPresent() ? opt.get() : save(new Metadata(mkey, value));
    }
}
