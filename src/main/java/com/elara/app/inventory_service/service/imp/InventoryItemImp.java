package com.elara.app.inventory_service.service.imp;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.mapper.InventoryItemMapper;
import com.elara.app.inventory_service.repository.InventoryItemRepository;
import com.elara.app.inventory_service.service.interfaces.InventoryItemService;
import com.elara.app.inventory_service.utils.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryItemImp implements InventoryItemService {

    private static final String ENTITY_NAME = "Inventory Item";
    private final InventoryItemMapper mapper;
    private final InventoryItemRepository repository;
    private final MessageService messageService;

    @Override
    @Transactional
    public InventoryItemResponse save(InventoryItemRequest request) {
        return null;
    }

    @Override
    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemUpdate update) {
        return null;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

    }

    @Override
    public InventoryItemResponse findById(Long id) {
        return null;
    }

    @Override
    public Page<InventoryItemResponse> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<InventoryItemResponse> findAllByName(String name, Pageable pageable) {
        return null;
    }

    @Override
    public Boolean isNameTaken(String name) {
        return null;
    }
}
