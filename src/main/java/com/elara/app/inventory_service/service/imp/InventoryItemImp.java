package com.elara.app.inventory_service.service.imp;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.exceptions.ResourceConflictException;
import com.elara.app.inventory_service.exceptions.ResourceNotFoundException;
import com.elara.app.inventory_service.mapper.InventoryItemMapper;
import com.elara.app.inventory_service.model.InventoryItem;
import com.elara.app.inventory_service.repository.InventoryItemRepository;
import com.elara.app.inventory_service.service.interfaces.InventoryItemService;
import com.elara.app.inventory_service.service.interfaces.UomServiceClient;
import com.elara.app.inventory_service.utils.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryItemImp implements InventoryItemService {

    private static final String ENTITY_NAME = "InventoryItem";
    private static final String NOMENCLATURE = ENTITY_NAME + "-service";
    private final InventoryItemMapper mapper;
    private final InventoryItemRepository repository;
    private final MessageService messageService;
    private final UomServiceClient uomServiceClient;

    @Override
    @Transactional
    public InventoryItemResponse save(InventoryItemRequest request) {
        final String methodNomenclature = NOMENCLATURE + "-save";
        log.info("[{}] Creating {} with name: '{}'", methodNomenclature, ENTITY_NAME, request.name());

        validateNameUniqueness(request.name());
        uomServiceClient.verifyUomById(request.baseUnitOfMeasureId());

        InventoryItem entity = mapper.toEntity(request);
        InventoryItem saved = repository.save(entity);

        log.info("[{}] {} created successfully with id: {}", methodNomenclature, ENTITY_NAME, saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemUpdate update) {
        final String methodNomenclature = NOMENCLATURE + "-update";
        log.info("[{}] Updating {} with id: {}", methodNomenclature, ENTITY_NAME, id);

        InventoryItem existing = findEntityById(id);
        validateNameUniquenessForUpdate(existing.getName(), update.name());
        uomServiceClient.verifyUomById(update.baseUnitOfMeasureId());

        mapper.updateEntityFromDto(existing, update);

        log.info("[{}] {} updated successfully with id: {}", methodNomenclature, ENTITY_NAME, id);
        return mapper.toResponse(existing);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        String methodNomenclature = NOMENCLATURE + "-deleteById";
        log.info("[{}] Attempting to delete {} with id: {}", methodNomenclature, ENTITY_NAME, id);
        if (!repository.existsById(id)) {
            String notFoundMsg = messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id);
            String deleteErrorMsg = messageService.getMessage("crud.delete.error", ENTITY_NAME);
            log.warn("[{}] {}", methodNomenclature, notFoundMsg);
            log.warn("[{}] {}", methodNomenclature, deleteErrorMsg);
            throw new ResourceNotFoundException(notFoundMsg);
        }
        repository.deleteById(id);
        String msg = messageService.getMessage("crud.delete.success", ENTITY_NAME);
        log.info("[{}] {}", methodNomenclature, msg);
    }

    @Override
    public InventoryItemResponse findById(Long id) {
        String methodNomenclature = NOMENCLATURE + "-findById";
        log.info("[{}] Searching {} with id: {}", methodNomenclature, ENTITY_NAME, id);
        Optional<InventoryItem> response = repository.findById(id);
        if (response.isEmpty()) {
            String msg = messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id.toString());
            log.warn("[{}] {}", methodNomenclature, msg);
            throw new ResourceNotFoundException(msg);
        }
        String msg = messageService.getMessage("crud.read.success", ENTITY_NAME);
        log.info("[{}] {}", methodNomenclature, msg);
        return mapper.toResponse(response.get());
    }

    @Override
    public Page<InventoryItemResponse> findAll(Pageable pageable) {
        String methodNomenclature = NOMENCLATURE + "-findAll";
        log.info("[{}] Fetching all {} entities", methodNomenclature, ENTITY_NAME);
        Page<InventoryItemResponse> page = repository.findAll(pageable).map(mapper::toResponse);
        log.info("[{}] Fetched {} entities, page size: {}", methodNomenclature, ENTITY_NAME, page.getNumberOfElements());
        return page;
    }

    @Override
    public Page<InventoryItemResponse> findAllByName(String name, Pageable pageable) {
        String methodNomenclature = NOMENCLATURE + "-findAllByName";
        log.info("[{}] Fetching all {} entities with name containing: '{}'", methodNomenclature, ENTITY_NAME, name);
        Page<InventoryItemResponse> page = repository.findAllByNameContainingIgnoreCase(name, pageable).map(mapper::toResponse);
        log.info("[{}] Fetched {} entities with name like '{}', page size: {}", methodNomenclature, ENTITY_NAME, name, page.getNumberOfElements());
        return page;
    }

    private boolean isNameTaken(String name) {
        String methodNomenclature = NOMENCLATURE + "-isNameTaken";
        log.debug("[{}] Checking if name '{}' is taken for {}", methodNomenclature, name, ENTITY_NAME);
        boolean exists = repository.existsByNameIgnoreCase(name);
        log.debug("[{}] Name '{}' taken: {}", methodNomenclature, name, exists);
        return exists;
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    private InventoryItem findEntityById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> {
                String message = messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id.toString());
                return new ResourceNotFoundException(message);
            });
    }

    private void validateNameUniqueness(String name) {
        if (isNameTaken(name)) {
            String message = messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", name);
            throw new ResourceConflictException(message);
        }
    }

    private void validateNameUniquenessForUpdate(String currentName, String newName) {
        if (!currentName.equals(newName) && isNameTaken(newName)) {
            String message = messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", newName);
            throw new ResourceConflictException(message);
        }
    }
}
