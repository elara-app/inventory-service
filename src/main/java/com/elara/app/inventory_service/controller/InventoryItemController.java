package com.elara.app.inventory_service.controller;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.service.interfaces.InventoryItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.info("[" + NOMENCLATURE + "-create] Request to create InventoryItem: {}", request);
        InventoryItemResponse response = service.save(request);
        log.info("[" + NOMENCLATURE + "-create] InventoryItem created with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========================================
    // READ OPERATIONS
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> getById(
        @PathVariable @NotNull @Positive Long id
    ) {
        // Implement this method
        return null;
    }

}
