package com.elara.app.inventory_service.repository;

import com.elara.app.inventory_service.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Page<InventoryItem> findAllByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    @Modifying
    @Query("DELETE FROM inventory_item i WHERE i.id = :id")
    int deleteByIdReturningCount(@Param("id") Long id);

}
