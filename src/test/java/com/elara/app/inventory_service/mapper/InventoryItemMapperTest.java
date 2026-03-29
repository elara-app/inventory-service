package com.elara.app.inventory_service.mapper;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.model.InventoryItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryItemMapper")
class InventoryItemMapperTest {

    @Autowired
    private InventoryItemMapper mapper;

    // ========================================
    // REQUEST TO ENTITY MAPPING
    // ========================================

    @Nested
    @DisplayName("ToEntity - Request to Entity Mapping")
    class ToEntityTests {

        @Test
        @DisplayName("Map request to entity, all fields mapped correctly")
        void toEntity_withValidRequest_mapsAllFieldsCorrectly() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                    "Steel Bolt M10",
                    "High-strength steel bolt, metric M10 x 50mm",
                    1L,
                    new BigDecimal("2.50"),
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00")
            );

            // When
            InventoryItem entity = mapper.toEntity(request);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("Steel Bolt M10");
            assertThat(entity.getDescription()).isEqualTo("High-strength steel bolt, metric M10 x 50mm");
            assertThat(entity.getBaseUnitOfMeasureId()).isEqualTo(1L);
            assertThat(entity.getStandardCost()).isEqualByComparingTo("2.50");
            assertThat(entity.getUnitPerPurchaseUom()).isEqualByComparingTo("100.00");
            assertThat(entity.getReorderPointQuantity()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("Map request to entity, ID is not set")
        void toEntity_withValidRequest_idIsNull() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                    "Copper Wire 2mm",
                    "Electrical copper wire",
                    2L,
                    new BigDecimal("5.75"),
                    new BigDecimal("200.00"),
                    new BigDecimal("1000.00")
            );

            // When
            InventoryItem entity = mapper.toEntity(request);

            // Then
            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("Map request with null description, handles correctly")
        void toEntity_withNullDescription_handlesCorrectly() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                    "Item Without Description",
                    null,
                    1L,
                    new BigDecimal("10.00"),
                    new BigDecimal("50.00"),
                    new BigDecimal("200.00")
            );

            // When
            InventoryItem entity = mapper.toEntity(request);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("Item Without Description");
            assertThat(entity.getDescription()).isNull();
        }

        @Test
        @DisplayName("Map request with BigDecimal precision, preserves decimal places")
        void toEntity_withBigDecimalPrecision_preservesDecimalPlaces() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                    "Precision Item",
                    "Test item",
                    1L,
                    new BigDecimal("99.99"),
                    new BigDecimal("123.45"),
                    new BigDecimal("678.90")
            );

            // When
            InventoryItem entity = mapper.toEntity(request);

            // Then
            assertThat(entity.getStandardCost()).isEqualByComparingTo("99.99");
            assertThat(entity.getUnitPerPurchaseUom()).isEqualByComparingTo("123.45");
            assertThat(entity.getReorderPointQuantity()).isEqualByComparingTo("678.90");
        }
    }

    // ========================================
    // ENTITY TO RESPONSE MAPPING
    // ========================================

    @Nested
    @DisplayName("ToResponse - Entity to Response Mapping")
    class ToResponseTests {

        @Test
        @DisplayName("Map entity to response, all fields mapped correctly")
        void toResponse_withValidEntity_mapsAllFieldsCorrectly() {
            // Given
            InventoryItem entity = InventoryItem.builder()
                    .id(1L)
                    .name("Steel Bolt M10")
                    .description("High-strength steel bolt, metric M10 x 50mm")
                    .baseUnitOfMeasureId(5L)
                    .standardCost(new BigDecimal("2.50"))
                    .unitPerPurchaseUom(new BigDecimal("100.00"))
                    .reorderPointQuantity(new BigDecimal("500.00"))
                    .build();

            // When
            InventoryItemResponse response = mapper.toResponse(entity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Steel Bolt M10");
            assertThat(response.description()).isEqualTo("High-strength steel bolt, metric M10 x 50mm");
            assertThat(response.baseUnitOfMeasureId()).isEqualTo(5L);
            assertThat(response.standardCost()).isEqualByComparingTo("2.50");
            assertThat(response.unitPerPurchaseUom()).isEqualByComparingTo("100.00");
            assertThat(response.reorderPointQuantity()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("Map entity with null description, handles correctly")
        void toResponse_withNullDescription_handlesCorrectly() {
            // Given
            InventoryItem entity = InventoryItem.builder()
                    .id(2L)
                    .name("Item Without Description")
                    .description(null)
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("15.00"))
                    .unitPerPurchaseUom(new BigDecimal("75.00"))
                    .reorderPointQuantity(new BigDecimal("300.00"))
                    .build();

            // When
            InventoryItemResponse response = mapper.toResponse(entity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(2L);
            assertThat(response.name()).isEqualTo("Item Without Description");
            assertThat(response.description()).isNull();
        }

        @Test
        @DisplayName("Map entity with BigDecimal values, preserves precision")
        void toResponse_withBigDecimalValues_preservesPrecision() {
            // Given
            InventoryItem entity = InventoryItem.builder()
                    .id(3L)
                    .name("Precision Item")
                    .description("Test")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("99.99"))
                    .unitPerPurchaseUom(new BigDecimal("123.45"))
                    .reorderPointQuantity(new BigDecimal("678.90"))
                    .build();

            // When
            InventoryItemResponse response = mapper.toResponse(entity);

            // Then
            assertThat(response.standardCost()).isEqualByComparingTo("99.99");
            assertThat(response.unitPerPurchaseUom()).isEqualByComparingTo("123.45");
            assertThat(response.reorderPointQuantity()).isEqualByComparingTo("678.90");
        }
    }

    // ========================================
    // UPDATE ENTITY FROM DTO MAPPING
    // ========================================

    @Nested
    @DisplayName("UpdateEntityFromDto - Update Mapping")
    class UpdateEntityFromDtoTests {

        @Test
        @DisplayName("Update entity from DTO, all fields updated")
        void updateEntityFromDto_withValidUpdate_updatesAllFields() {
            // Given
            InventoryItem existing = InventoryItem.builder()
                    .id(1L)
                    .name("Old Name")
                    .description("Old Description")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("10.00"))
                    .unitPerPurchaseUom(new BigDecimal("50.00"))
                    .reorderPointQuantity(new BigDecimal("200.00"))
                    .build();

            InventoryItemUpdate update = new InventoryItemUpdate(
                    "New Name",
                    "New Description",
                    2L,
                    new BigDecimal("20.00"),
                    new BigDecimal("100.00"),
                    new BigDecimal("400.00")
            );

            // When
            mapper.updateEntityFromDto(existing, update);

            // Then
            assertThat(existing.getName()).isEqualTo("New Name");
            assertThat(existing.getDescription()).isEqualTo("New Description");
            assertThat(existing.getBaseUnitOfMeasureId()).isEqualTo(2L);
            assertThat(existing.getStandardCost()).isEqualByComparingTo("20.00");
            assertThat(existing.getUnitPerPurchaseUom()).isEqualByComparingTo("100.00");
            assertThat(existing.getReorderPointQuantity()).isEqualByComparingTo("400.00");
        }

        @Test
        @DisplayName("Update entity from DTO, ID preserved")
        void updateEntityFromDto_withValidUpdate_preservesId() {
            // Given
            InventoryItem existing = InventoryItem.builder()
                    .id(10L)
                    .name("Old Name")
                    .description("Old Description")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("5.00"))
                    .unitPerPurchaseUom(new BigDecimal("25.00"))
                    .reorderPointQuantity(new BigDecimal("100.00"))
                    .build();

            InventoryItemUpdate update = new InventoryItemUpdate(
                    "Updated Name",
                    "Updated Description",
                    3L,
                    new BigDecimal("15.00"),
                    new BigDecimal("75.00"),
                    new BigDecimal("300.00")
            );

            // When
            mapper.updateEntityFromDto(existing, update);

            // Then
            assertThat(existing.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Update entity with null description, handles correctly")
        void updateEntityFromDto_withNullDescription_handlesCorrectly() {
            // Given
            InventoryItem existing = InventoryItem.builder()
                    .id(5L)
                    .name("Item With Description")
                    .description("Original Description")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("8.00"))
                    .unitPerPurchaseUom(new BigDecimal("40.00"))
                    .reorderPointQuantity(new BigDecimal("150.00"))
                    .build();

            InventoryItemUpdate update = new InventoryItemUpdate(
                    "Updated Name",
                    null,
                    1L,
                    new BigDecimal("8.00"),
                    new BigDecimal("40.00"),
                    new BigDecimal("150.00")
            );

            // When
            mapper.updateEntityFromDto(existing, update);

            // Then
            assertThat(existing.getName()).isEqualTo("Updated Name");
            assertThat(existing.getDescription()).isNull();
        }

        @Test
        @DisplayName("Update entity with different values, reflects changes")
        void updateEntityFromDto_withDifferentValues_reflectsChanges() {
            // Given
            InventoryItem existing = InventoryItem.builder()
                    .id(7L)
                    .name("Bolt M8")
                    .description("Small bolt")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("1.50"))
                    .unitPerPurchaseUom(new BigDecimal("200.00"))
                    .reorderPointQuantity(new BigDecimal("1000.00"))
                    .build();

            InventoryItemUpdate update = new InventoryItemUpdate(
                    "Bolt M12",
                    "Large bolt",
                    2L,
                    new BigDecimal("3.50"),
                    new BigDecimal("150.00"),
                    new BigDecimal("800.00")
            );

            // When
            mapper.updateEntityFromDto(existing, update);

            // Then
            assertThat(existing.getId()).isEqualTo(7L);
            assertThat(existing.getName()).isEqualTo("Bolt M12");
            assertThat(existing.getDescription()).isEqualTo("Large bolt");
            assertThat(existing.getBaseUnitOfMeasureId()).isEqualTo(2L);
            assertThat(existing.getStandardCost()).isEqualByComparingTo("3.50");
            assertThat(existing.getUnitPerPurchaseUom()).isEqualByComparingTo("150.00");
            assertThat(existing.getReorderPointQuantity()).isEqualByComparingTo("800.00");
        }
    }

    // ========================================
    // EDGE CASES AND BOUNDARY VALUES
    // ========================================

    @Nested
    @DisplayName("Edge Cases and Boundary Values")
    class EdgeCaseTests {

        @Test
        @DisplayName("Map request with maximum field lengths, handles correctly")
        void toEntity_withMaximumFieldLengths_handlesCorrectly() {
            // Given - Name max 100, Description max 200
            String maxName = "A".repeat(100);
            String maxDescription = "B".repeat(200);
            InventoryItemRequest request = new InventoryItemRequest(
                    maxName,
                    maxDescription,
                    1L,
                    new BigDecimal("999999999.99"),
                    new BigDecimal("999999999.99"),
                    new BigDecimal("999999999.99")
            );

            // When
            InventoryItem entity = mapper.toEntity(request);

            // Then
            assertThat(entity.getName()).hasSize(100);
            assertThat(entity.getDescription()).hasSize(200);
            assertThat(entity.getStandardCost()).isEqualByComparingTo("999999999.99");
        }

        @Test
        @DisplayName("Map entity with minimum BigDecimal values, handles correctly")
        void toResponse_withMinimumBigDecimalValues_handlesCorrectly() {
            // Given
            InventoryItem entity = InventoryItem.builder()
                    .id(1L)
                    .name("Minimum Values Item")
                    .description("Test minimum values")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("0.01"))
                    .unitPerPurchaseUom(new BigDecimal("0.01"))
                    .reorderPointQuantity(new BigDecimal("0.01"))
                    .build();

            // When
            InventoryItemResponse response = mapper.toResponse(entity);

            // Then
            assertThat(response.standardCost()).isEqualByComparingTo("0.01");
            assertThat(response.unitPerPurchaseUom()).isEqualByComparingTo("0.01");
            assertThat(response.reorderPointQuantity()).isEqualByComparingTo("0.01");
        }

        @Test
        @DisplayName("Update entity with empty description string, sets to empty")
        void updateEntityFromDto_withEmptyDescription_setsToEmpty() {
            // Given
            InventoryItem existing = InventoryItem.builder()
                    .id(1L)
                    .name("Item With Description")
                    .description("Original Description")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("5.00"))
                    .unitPerPurchaseUom(new BigDecimal("25.00"))
                    .reorderPointQuantity(new BigDecimal("100.00"))
                    .build();

            InventoryItemUpdate update = new InventoryItemUpdate(
                    "Item With Description",
                    "",
                    1L,
                    new BigDecimal("5.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("100.00")
            );

            // When
            mapper.updateEntityFromDto(existing, update);

            // Then
            assertThat(existing.getDescription()).isEmpty();
        }
    }
}
