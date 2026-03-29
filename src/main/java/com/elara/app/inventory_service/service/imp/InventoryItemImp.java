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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        final String methodNomenclature = NOMENCLATURE + "-deleteById";
        log.info("[{}] Deleting {} with id: {}", methodNomenclature, ENTITY_NAME, id);

        int deletedCount = repository.deleteByIdReturningCount(id);
        if (deletedCount == 0) {
            String message = messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id.toString());
            throw new ResourceNotFoundException(message);
        }

        log.info("[{}] {} deleted successfully with id: {}", methodNomenclature, ENTITY_NAME, id);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse findById(Long id) {
        final String methodNomenclature = NOMENCLATURE + "-findById";
        log.debug("[{}] Fetching {} with id: {}", methodNomenclature, ENTITY_NAME, id);

        InventoryItem entity = findEntityById(id);

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAll(Pageable pageable) {
        final String methodNomenclature = NOMENCLATURE + "-findAll";
        log.debug("[{}] Fetching all {} entities", methodNomenclature, ENTITY_NAME);

        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAllByName(String name, Pageable pageable) {
        final String methodNomenclature = NOMENCLATURE + "-findAllByName";
        log.debug("[{}] Fetching {} entities with name containing: '{}'", methodNomenclature, ENTITY_NAME, name);

        return repository.findAllByNameContainingIgnoreCase(name, pageable).map(mapper::toResponse);
    }

    private boolean isNameTaken(String name) {
        return repository.existsByNameIgnoreCase(name);
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
