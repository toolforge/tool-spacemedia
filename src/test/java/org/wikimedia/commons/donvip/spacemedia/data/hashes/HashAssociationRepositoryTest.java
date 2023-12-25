package org.wikimedia.commons.donvip.spacemedia.data.hashes;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackageClasses = HashAssociation.class)
@EnableJpaRepositories(basePackageClasses = HashAssociationRepository.class, includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = HashAssociationRepository.class))
class HashAssociationRepositoryTest extends HashesTestDataJpa {

    @Autowired
    private HashAssociationRepository repository;

    @Test
    void injectedRepositoriesAreNotNull() {
        checkInjectedComponentsAreNotNull();
        assertNotNull(repository);
    }
}
