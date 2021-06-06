package org.wikimedia.commons.donvip.spacemedia.data.jpa.repository;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Person;

public interface DepotRepository extends CrudRepository<Depot, String> {

    default Depot findOrCreate(String id, String name, URL url, Set<? extends Person> operators) {
        Optional<Depot> opt = findById(id);
        return opt.isPresent() ? opt.get() : save(new Depot(id, name, url, operators));
    }
}
