package com.elara.app.inventory_service.controller;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.service.interfaces.InventoryItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/inventory-item", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Slf4j
// @Tag(
public class InventoryItemController {

    private final InventoryItemService service;
    private static final String NOMENCLATURE = "InventoryItem-controller";

    // ========================================
    // CREATE OPERATIONS
    // ========================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItemResponse> create(
        @Valid @RequestBody InventoryItemRequest request
    ) {
        final String methodNomenclature = NOMENCLATURE + "-create";
        log.info("[{}] Request to create InventoryItem: {}", methodNomenclature, request);
        InventoryItemResponse response = service.save(request);
        log.info("[{}] InventoryItem created with id: {}", methodNomenclature, response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========================================
    // READ OPERATIONS
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> getById(
        @PathVariable @NotNull @Positive Long id
    ) {
        final String methodNomenclature = NOMENCLATURE + "-getById";
        log.info("[{}] Request to get InventoryItem by id: {}", methodNomenclature, id);
        InventoryItemResponse response = service.findById(id);
        log.info("[{}] InventoryItem found: {}", methodNomenclature, response);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<InventoryItemResponse>> getAll(
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        final String methodNomenclature = NOMENCLATURE + "-getAll";
        log.info("[{}] Request to get all InventoryItems.", methodNomenclature);
        Page<InventoryItemResponse> response = service.findAll(pageable);
        log.info("[{}] Fetched {} InventoryItems.", methodNomenclature, response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<InventoryItemResponse>> getAllByName(
        @RequestParam @NotBlank String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        final String methodNomenclature = NOMENCLATURE + "-getAllByName";
        log.info("[{}] Request to search InventoryItems by name: {}", methodNomenclature, name);
        Page<InventoryItemResponse> response = service.findAllByName(name, pageable);
        log.info("[{}] Fetched {} InventoryItems for name: '{}'", methodNomenclature, response.getNumberOfElements(), name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-name")
    public ResponseEntity<Boolean> isNameTaken(
        @RequestParam @NotBlank String name
    ) {
        final String methodNomenclature = NOMENCLATURE + "-isNameTaken";
        log.info("[{}] Request to check if name is taken: {}", methodNomenclature, name);
        Boolean isTaken = service.isNameTaken(name);
        log.info("[{}] Name: {}", methodNomenclature, isTaken);
        return ResponseEntity.ok(isTaken);
    }

}
