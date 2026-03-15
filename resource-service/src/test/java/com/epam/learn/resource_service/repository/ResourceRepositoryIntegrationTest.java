package com.epam.learn.resource_service.repository;

import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.enumeration.ResourceState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ResourceRepository Integration Tests")
class ResourceRepositoryIntegrationTest {

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    @DisplayName("Should persist and load ResourceEntity")
    void save_shouldPersistAndAssignId() {
        ResourceEntity saved = resourceRepository.saveAndFlush(ResourceEntity.builder()
                .storageKey("k1")
                .state(ResourceState.STAGING)
                .build());

        assertNotNull(saved.getId());
        ResourceEntity found = resourceRepository.findById(saved.getId()).orElseThrow();
        assertEquals("k1", found.getStorageKey());
    }

    @Test
    @DisplayName("Should return only existing IDs")
    void findExistingIds_shouldReturnOnlyExisting() {
        ResourceEntity r1 = resourceRepository.saveAndFlush(ResourceEntity.builder()
                .storageKey("k1")
                .state(ResourceState.STAGING)
                .build());
        ResourceEntity r2 = resourceRepository.saveAndFlush(ResourceEntity.builder()
                .storageKey("k2")
                .state(ResourceState.STAGING)
                .build());

        List<Long> existing = resourceRepository.findExistingIds(List.of(r1.getId(), 9999L, r2.getId()));

        assertEquals(2, existing.size());
        assertTrue(existing.containsAll(List.of(r1.getId(), r2.getId())));
        assertFalse(existing.contains(9999L));
    }
}
