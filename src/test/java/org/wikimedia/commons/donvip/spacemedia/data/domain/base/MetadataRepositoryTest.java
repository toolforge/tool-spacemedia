package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wikimedia.commons.donvip.spacemedia.data.domain.TestDataJpa;

@EntityScan(basePackageClasses = FileMetadata.class)
@EnableJpaRepositories(basePackageClasses = FileMetadataRepository.class, includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FileMetadataRepository.class))
class MetadataRepositoryTest extends TestDataJpa {

    @Autowired
    private FileMetadataRepository repository;

    @Test
    void injectedRepositoriesAreNotNull() {
        checkInjectedComponentsAreNotNull();
        assertNotNull(repository);
    }
}
