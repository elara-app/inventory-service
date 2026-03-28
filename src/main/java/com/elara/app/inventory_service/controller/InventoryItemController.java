package com.elara.app.inventory_service.controller;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.service.interfaces.InventoryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(value = "item/", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(
    name = "Inventory Item Management",
    description = "Complete API for managing Inventory Items. Includes CRUD operations, pagination, search, " +
                  "name validation, and cost management."
)
public class InventoryItemController {

    private final InventoryItemService service;
    private static final String ENTITY_NAME = "InventoryItem";
    private static final String NOMENCLATURE = ENTITY_NAME + "-controller";

    // ========================================
    // CREATE OPERATIONS
    // ========================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create new Inventory Item", description = """
            Creates a new Inventory Item record.
            
            **Validation Rules:**
            - `name`: Required, max 100 characters, must be unique
            - `description`: Optional, max 200 characters
            - `baseUnitOfMeasureId`: Required, positive ID of an existing UOM
            - `standardCost`: Required, positive number
            - `unitPerPurchaseUom`: Required, positive number
            - `reorderPointQuantity`: Required, positive number""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created successfully - Returns the newly created item",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemResponse"),
                examples = @ExampleObject(name = "Success", ref = "#/components/examples/InventoryItemCreated"))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Validation failed",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Validation Error", ref = "#/components/examples/ErrorBadRequestInventoryItem"))),
        @ApiResponse(responseCode = "409", description = "Conflict - Item with same name already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Name Conflict", ref = "#/components/examples/ErrorInventoryItemConflict"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<InventoryItemResponse> create(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Payload for creating an Inventory Item",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemRequest"),
                examples = @ExampleObject(name = "Create Request",
                    value = "{\"name\":\"Steel Bolt M10\",\"description\":\"High-strength steel bolt, metric M10 x 50mm\",\"baseUnitOfMeasureId\":1,\"standardCost\":2.50,\"unitPerPurchaseUom\":100.00,\"reorderPointQuantity\":500.00}"))
        )
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

    @GetMapping("{id}")
    @Operation(summary = "Get Inventory Item by ID", description = "Retrieves a single Inventory Item by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success - Returns the requested item",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemResponse"),
                examples = @ExampleObject(name = "Success", ref = "#/components/examples/InventoryItemCreated"))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID format",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Bad Request", ref = "#/components/examples/ErrorBadRequest"))),
        @ApiResponse(responseCode = "404", description = "Not Found - Item does not exist",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Not Found", ref = "#/components/examples/ErrorInventoryItemNotFound"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<InventoryItemResponse> getById(
        @Parameter(description = "Inventory Item ID", required = true, example = "1")
        @PathVariable @NotNull @Positive Long id
    ) {
        final String methodNomenclature = NOMENCLATURE + "-getById";
        log.info("[{}] Request to get InventoryItem by id: {}", methodNomenclature, id);
        InventoryItemResponse response = service.findById(id);
        log.info("[{}] InventoryItem found: {}", methodNomenclature, response);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all Inventory Items", description = """
            Retrieves a paginated list of all Inventory Items.
            
            **Pagination Parameters:**
            - `page`: Page number (0-indexed, default: 0)
            - `size`: Page size (default: 20)
            - `sort`: Sort field and direction (default: name,asc)""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success - Returns paginated items",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemPageResponse"),
                examples = @ExampleObject(name = "Success", ref = "#/components/examples/InventoryItemPage"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<Page<InventoryItemResponse>> getAll(
        @Parameter(description = "Pagination parameters", hidden = true)
        @PageableDefault(size = 20) Pageable pageable
    ) {
        final String methodNomenclature = NOMENCLATURE + "-getAll";
        log.info("[{}] Request to get all InventoryItems.", methodNomenclature);
        Page<InventoryItemResponse> response = service.findAll(pageable);
        log.info("[{}] Fetched {} InventoryItems.", methodNomenclature, response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("search")
    @Operation(summary = "Search Inventory Items by name", description = """
            Searches for Inventory Items by name using case-insensitive partial matching.
            Returns a paginated list of matching items.
            
            **Example:** Searching for "bolt" will match "Steel Bolt M10", "Bolt M8", "High-strength bolt", etc.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success - Returns paginated matching items",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemPageResponse"),
                examples = @ExampleObject(name = "Success", ref = "#/components/examples/InventoryItemPage"))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Name parameter is blank",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Bad Request", ref = "#/components/examples/ErrorBadRequest"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<Page<InventoryItemResponse>> getAllByName(
        @Parameter(description = "Name search term (case-insensitive partial match)", required = true, example = "bolt")
        @RequestParam @NotBlank String name,
        @Parameter(description = "Pagination parameters", hidden = true)
        @PageableDefault(size = 20) Pageable pageable
    ) {
        final String methodNomenclature = NOMENCLATURE + "-getAllByName";
        log.info("[{}] Request to search InventoryItems by name: {}", methodNomenclature, name);
        Page<InventoryItemResponse> response = service.findAllByName(name, pageable);
        log.info("[{}] Fetched {} InventoryItems for name: '{}'", methodNomenclature, response.getNumberOfElements(), name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("check-name")
    @Operation(summary = "Check if name is taken", description = """
            Checks whether an Inventory Item with the given name already exists.
            Useful for client-side validation before creating or updating items.
            
            **Returns:** `true` if name is taken, `false` if available""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success - Returns boolean result",
            content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Name parameter is blank",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Bad Request", ref = "#/components/examples/ErrorBadRequest"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<Boolean> isNameTaken(
        @Parameter(description = "Name to check for uniqueness", required = true, example = "Steel Bolt M10")
        @RequestParam @NotBlank String name
    ) {
        final String methodNomenclature = NOMENCLATURE + "-isNameTaken";
        log.info("[{}] Request to check if name is taken: {}", methodNomenclature, name);
        Boolean isTaken = service.isNameTaken(name);
        log.info("[{}] Name: {}", methodNomenclature, isTaken);
        return ResponseEntity.ok(isTaken);
    }

    // ========================================
    // UPDATE OPERATIONS
    // ========================================

    @PutMapping("{id}")
    @Operation(summary = "Update Inventory Item", description = """
            Updates an existing Inventory Item by ID.
            All fields in the request body are required.
            
            **Note:** Name uniqueness is validated (excluding the current item)""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success - Returns the updated item",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemResponse"),
                examples = @ExampleObject(name = "Success", ref = "#/components/examples/InventoryItemUpdated"))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Validation failed",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Bad Request", ref = "#/components/examples/ErrorBadRequestInventoryItem"))),
        @ApiResponse(responseCode = "404", description = "Not Found - Item does not exist",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Not Found", ref = "#/components/examples/ErrorInventoryItemNotFound"))),
        @ApiResponse(responseCode = "409", description = "Conflict - Name already taken by another item",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Name Conflict", ref = "#/components/examples/ErrorInventoryItemConflict"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<InventoryItemResponse> update(
        @Parameter(description = "Inventory Item ID", required = true, example = "1")
        @PathVariable @NotNull @Positive Long id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Payload for updating an Inventory Item",
            content = @Content(schema = @Schema(ref = "#/components/schemas/InventoryItemUpdate"),
                examples = @ExampleObject(name = "Update Request",
                    value = "{\"name\":\"Steel Bolt M12\",\"description\":\"High-strength steel bolt, metric M12 x 50mm\",\"baseUnitOfMeasureId\":1,\"standardCost\":3.00,\"unitPerPurchaseUom\":100.00,\"reorderPointQuantity\":400.00}"))
        )
        @Valid @RequestBody InventoryItemUpdate update
    ) {
        final String methodNomenclature = NOMENCLATURE + "-update";
        log.info("[{}] Request to update InventoryItem; id: {}, with data: {}", methodNomenclature, id, update);
        InventoryItemResponse response = service.update(id, update);
        log.info("[{}] InventoryItem updated: {}", methodNomenclature, response);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    @DeleteMapping("{id}")
    @Operation(summary = "Delete Inventory Item", description = """
            Deletes an Inventory Item by ID.
            
            **Note:** Deletion may fail if the item is referenced by other records (e.g., purchase orders, transactions)""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "No Content - Item successfully deleted"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID format",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Bad Request", ref = "#/components/examples/ErrorBadRequest"))),
        @ApiResponse(responseCode = "404", description = "Not Found - Item does not exist",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Not Found", ref = "#/components/examples/ErrorInventoryItemNotFound"))),
        @ApiResponse(responseCode = "409", description = "Conflict - Item is referenced by other records",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Delete Conflict", ref = "#/components/examples/ErrorDeleteConflict"))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(name = "Server Error", ref = "#/components/examples/ErrorServer")))
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Inventory Item ID", required = true, example = "1")
        @PathVariable @NotNull @Positive Long id
    ) {
        final String methodNomenclature = NOMENCLATURE + "-delete";
        log.info("[{}] Request to delete InventoryItem id: {}", methodNomenclature, id);
        service.deleteById(id);
        log.info("[{}] InventoryItem deleted: {}", methodNomenclature, id);
        return ResponseEntity.noContent().build();
    }

}
