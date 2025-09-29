package com.elara.app.inventory_service.repository;

import com.elara.app.inventory_service.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Page<InventoryItem> findAllByNameContainingIgnoreCase(String name, Pageable pageable);

    Boolean existsByNameIgnoreCase(String name);

}
