package com.elara.app.inventory_service.service.interfaces;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryItemService {

    InventoryItemResponse save(InventoryItemRequest request);

    InventoryItemResponse update(Long id, InventoryItemUpdate update);

    void deleteById(Long id);

    InventoryItemResponse findById(Long id);

    Page<InventoryItemResponse> findAll(Pageable pageable);

    Page<InventoryItemResponse> findAllByName(String name, Pageable pageable);

    Boolean isNameTaken(String name);

}
