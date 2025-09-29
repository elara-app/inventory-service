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
import com.elara.app.inventory_service.utils.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryItemImp implements InventoryItemService {

    private static final String ENTITY_NAME = "Inventory Item";
    private static final String NOMENCLATURE = "InventoryItem-service";
    private final InventoryItemMapper mapper;
    private final InventoryItemRepository repository;
    private final MessageService messageService;
    private final UomServiceClient uomServiceClient;

    @Override
    @Transactional
    public InventoryItemResponse save(InventoryItemRequest request) {
        final String methodNomenclature = NOMENCLATURE + "-save";
        try {
            log.debug("[{}] Attempting to create {} with name: {} and request: {}", methodNomenclature, ENTITY_NAME, request != null ? request.name() : null, request);
            if (Boolean.TRUE.equals(isNameTaken(Objects.requireNonNull(request).name()))) {
                String msg = messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", request.name());
                log.warn("[{}] {}", methodNomenclature, msg);
                throw new ResourceConflictException(new Object[]{"name", request.name()});
            }
            uomServiceClient.verifyUomById(request.baseUnitOfMeasureId());
            InventoryItem entity = mapper.toEntity(request);
            log.debug("[" + NOMENCLATURE + "save] Mapped DTO to entity: {}", entity);
            InventoryItem saved = repository.save(entity);
            log.debug("[" + NOMENCLATURE + "save] {}", messageService.getMessage("crud.create.success", ENTITY_NAME));
            return mapper.toResponse(saved);
        } catch (ResourceConflictException | ResourceNotFoundException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("[{}] Data integrity violation while saving {}: {}", methodNomenclature, ENTITY_NAME, e.getMessage());
            throw new ResourceConflictException(e.getMessage());
        } catch (Exception e) {
            log.error("[{}] Unexpected error while saving: {}: {}", methodNomenclature, ENTITY_NAME, e.getMessage(), e);
        }
        return null;
    }

    @Override
    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemUpdate update) {
        // Implement update
        return null;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // Implement delete
    }

    @Override
    public InventoryItemResponse findById(Long id) {
        String methodNomenclature = NOMENCLATURE + "-findById";
        log.debug("[{}] Searching {} with id: {}", methodNomenclature, ENTITY_NAME, id);
        Optional<InventoryItemResponse> response = repository.findById(id).map(mapper::toResponse);
        if (response.isEmpty()) {
            String msg = messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id.toString());
            log.warn("[{}] {}", methodNomenclature, msg);
            throw new ResourceNotFoundException(new Object[]{"id", id.toString()});
        }
        String msg = messageService.getMessage("crud.read.success", ENTITY_NAME);
        log.debug("[{}] {}", methodNomenclature, msg);
        return response.get();
    }

    @Override
    public Page<InventoryItemResponse> findAll(Pageable pageable) {
        // Implement findAll
        return null;
    }

    @Override
    public Page<InventoryItemResponse> findAllByName(String name, Pageable pageable) {
        // Implement findAllByName
        return null;
    }

    @Override
    public Boolean isNameTaken(String name) {
        String methodNomenclature = NOMENCLATURE + "-isNameTaken";
        log.debug("[{}] checking if name '{}' is taken for {}", methodNomenclature, name, ENTITY_NAME);
        Boolean exists = repository.existsByNameIgnoreCase(name);
        log.debug("[{}] Name '{}' taken: {}", methodNomenclature, name, exists);
        return exists;
    }
}
