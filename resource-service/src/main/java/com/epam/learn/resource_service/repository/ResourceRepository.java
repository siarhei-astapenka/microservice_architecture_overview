package com.epam.learn.resource_service.repository;

import com.epam.learn.resource_service.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {
    @Query("SELECT r.id FROM ResourceEntity r WHERE r.id IN :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);
}
