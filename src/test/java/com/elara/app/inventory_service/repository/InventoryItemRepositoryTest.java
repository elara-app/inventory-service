package com.elara.app.inventory_service.repository;

import com.elara.app.inventory_service.model.InventoryItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("InventoryItemRepository")
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    // ========================================
    // HELPER METHODS
    // ========================================

    private InventoryItem createAndPersistItem(String name, String description, Long baseUomId,
                                               BigDecimal standardCost, BigDecimal unitPerPurchase,
                                               BigDecimal reorderPoint) {
        InventoryItem item = InventoryItem.builder()
                .name(name)
                .description(description)
                .baseUnitOfMeasureId(baseUomId)
                .standardCost(standardCost)
                .unitPerPurchaseUom(unitPerPurchase)
                .reorderPointQuantity(reorderPoint)
                .build();
        entityManager.persist(item);
        entityManager.flush();
        return item;
    }

    private InventoryItem createStandardItem(String name) {
        return createAndPersistItem(
                name,
                "Test description for " + name,
                1L,
                new BigDecimal("25.50"),
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
        );
    }

    // ========================================
    // BASIC CRUD OPERATIONS
    // ========================================

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Save inventory item, persists and returns entity with generated ID")
        void save_withValidItem_persistsAndReturnsEntityWithId() {
            // Given
            InventoryItem item = InventoryItem.builder()
                    .name("Steel Bolt M10")
                    .description("High-strength steel bolt, metric M10 x 50mm")
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("2.50"))
                    .unitPerPurchaseUom(new BigDecimal("100.00"))
                    .reorderPointQuantity(new BigDecimal("500.00"))
                    .build();

            // When
            InventoryItem saved = repository.save(item);
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Steel Bolt M10");
            assertThat(saved.getDescription()).isEqualTo("High-strength steel bolt, metric M10 x 50mm");
            assertThat(saved.getBaseUnitOfMeasureId()).isEqualTo(1L);
            assertThat(saved.getStandardCost()).isEqualByComparingTo("2.50");
            assertThat(saved.getUnitPerPurchaseUom()).isEqualByComparingTo("100.00");
            assertThat(saved.getReorderPointQuantity()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("FindById with existing ID, returns item")
        void findById_withExistingId_returnsItem() {
            // Given
            InventoryItem item = createStandardItem("Copper Wire 2mm");
            Long itemId = item.getId();
            entityManager.clear();

            // When
            Optional<InventoryItem> found = repository.findById(itemId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(itemId);
            assertThat(found.get().getName()).isEqualTo("Copper Wire 2mm");
        }

        @Test
        @DisplayName("FindById with non-existent ID, returns empty")
        void findById_withNonExistentId_returnsEmpty() {
            // When
            Optional<InventoryItem> found = repository.findById(999L);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("DeleteById removes item from database")
        void deleteById_removesItemFromDatabase() {
            // Given
            InventoryItem item = createStandardItem("Wooden Plank 2x4");
            Long itemId = item.getId();
            entityManager.clear();

            // When
            repository.deleteById(itemId);
            entityManager.flush();

            // Then
            Optional<InventoryItem> found = repository.findById(itemId);
            assertThat(found).isEmpty();
        }
    }

    // ========================================
    // NAME UNIQUENESS CHECKS
    // ========================================

    @Nested
    @DisplayName("ExistsByNameIgnoreCase - Name Uniqueness")
    class ExistsByNameTests {

        @Test
        @DisplayName("Existing name with exact match, returns true")
        void existsByNameIgnoreCase_withExactMatch_returnsTrue() {
            // Given
            createStandardItem("Paint Gallon White");

            // When
            Boolean exists = repository.existsByNameIgnoreCase("Paint Gallon White");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Existing name with different case, returns true")
        void existsByNameIgnoreCase_withDifferentCase_returnsTrue() {
            // Given
            createStandardItem("Aluminum Sheet 1mm");

            // When
            Boolean existsLower = repository.existsByNameIgnoreCase("aluminum sheet 1mm");
            Boolean existsUpper = repository.existsByNameIgnoreCase("ALUMINUM SHEET 1MM");
            Boolean existsMixed = repository.existsByNameIgnoreCase("aLuMiNuM sHeEt 1mM");

            // Then
            assertThat(existsLower).isTrue();
            assertThat(existsUpper).isTrue();
            assertThat(existsMixed).isTrue();
        }

        @Test
        @DisplayName("Non-existent name, returns false")
        void existsByNameIgnoreCase_withNonExistentName_returnsFalse() {
            // Given
            createStandardItem("Concrete Mix 50kg");

            // When
            Boolean exists = repository.existsByNameIgnoreCase("Non Existent Item");

            // Then
            assertThat(exists).isFalse();
        }
    }

    // ========================================
    // SEARCH BY NAME
    // ========================================

    @Nested
    @DisplayName("FindAllByNameContainingIgnoreCase - Search Operations")
    class SearchByNameTests {

        @Test
        @DisplayName("Partial match search, returns matching items")
        void findAllByNameContainingIgnoreCase_withPartialMatch_returnsMatchingItems() {
            // Given
            createStandardItem("Steel Bolt M10");
            createStandardItem("Steel Washer M10");
            createStandardItem("Steel Nut M10");
            createStandardItem("Aluminum Bolt M10");
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<InventoryItem> result = repository.findAllByNameContainingIgnoreCase("steel", pageable);

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .extracting(InventoryItem::getName)
                    .allMatch(name -> name.toLowerCase().contains("steel"));
        }

        @Test
        @DisplayName("Case insensitive search, returns matching items")
        void findAllByNameContainingIgnoreCase_caseInsensitive_returnsMatchingItems() {
            // Given
            createStandardItem("Copper Wire 2mm");
            createStandardItem("Copper Pipe 15mm");
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<InventoryItem> resultLower = repository.findAllByNameContainingIgnoreCase("copper", pageable);
            Page<InventoryItem> resultUpper = repository.findAllByNameContainingIgnoreCase("COPPER", pageable);
            Page<InventoryItem> resultMixed = repository.findAllByNameContainingIgnoreCase("CoPpEr", pageable);

            // Then
            assertThat(resultLower.getContent()).hasSize(2);
            assertThat(resultUpper.getContent()).hasSize(2);
            assertThat(resultMixed.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Search with no matches, returns empty page")
        void findAllByNameContainingIgnoreCase_withNoMatches_returnsEmptyPage() {
            // Given
            createStandardItem("Plastic Container 5L");
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<InventoryItem> result = repository.findAllByNameContainingIgnoreCase("nonexistent", pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Search with empty database, returns empty page")
        void findAllByNameContainingIgnoreCase_withEmptyDatabase_returnsEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<InventoryItem> result = repository.findAllByNameContainingIgnoreCase("anything", pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ========================================
    // PAGINATION
    // ========================================

    @Nested
    @DisplayName("Pagination Support")
    class PaginationTests {

        @Test
        @DisplayName("Paginated search, returns correct page size and content")
        void findAllByNameContainingIgnoreCase_withPagination_returnsCorrectPage() {
            // Given
            for (int i = 1; i <= 15; i++) {
                createStandardItem("Item " + i);
            }

            // When
            Page<InventoryItem> page1 = repository.findAllByNameContainingIgnoreCase("item", PageRequest.of(0, 10));
            Page<InventoryItem> page2 = repository.findAllByNameContainingIgnoreCase("item", PageRequest.of(1, 10));

            // Then
            assertThat(page1.getContent()).hasSize(10);
            assertThat(page1.getTotalElements()).isEqualTo(15);
            assertThat(page1.getTotalPages()).isEqualTo(2);
            assertThat(page1.isFirst()).isTrue();
            assertThat(page1.hasNext()).isTrue();

            assertThat(page2.getContent()).hasSize(5);
            assertThat(page2.getTotalElements()).isEqualTo(15);
            assertThat(page2.getTotalPages()).isEqualTo(2);
            assertThat(page2.isLast()).isTrue();
            assertThat(page2.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("FindAll with pagination, returns correct page")
        void findAll_withPagination_returnsCorrectPage() {
            // Given
            createStandardItem("Bolt M8");
            createStandardItem("Bolt M10");
            createStandardItem("Bolt M12");
            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<InventoryItem> result = repository.findAll(pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Save item with null description, persists successfully")
        void save_withNullDescription_persistsSuccessfully() {
            // Given
            InventoryItem item = InventoryItem.builder()
                    .name("Item Without Description")
                    .description(null)
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("10.00"))
                    .unitPerPurchaseUom(new BigDecimal("50.00"))
                    .reorderPointQuantity(new BigDecimal("200.00"))
                    .build();

            // When
            InventoryItem saved = repository.save(item);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<InventoryItem> found = repository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getDescription()).isNull();
        }

        @Test
        @DisplayName("Search with special characters, handles correctly")
        void findAllByNameContainingIgnoreCase_withSpecialCharacters_handlesCorrectly() {
            // Given
            createStandardItem("Bolt M10 (Zinc-Plated)");
            createStandardItem("Wire 2.5mm [Heavy-Duty]");
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<InventoryItem> resultParentheses = repository.findAllByNameContainingIgnoreCase("zinc-plated", pageable);
            Page<InventoryItem> resultBrackets = repository.findAllByNameContainingIgnoreCase("heavy-duty", pageable);

            // Then
            assertThat(resultParentheses.getContent()).hasSize(1);
            assertThat(resultBrackets.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Save item with maximum field lengths, persists successfully")
        void save_withMaximumFieldLengths_persistsSuccessfully() {
            // Given - Name max 100, Description max 200
            String maxName = "A".repeat(100);
            String maxDescription = "B".repeat(200);
            InventoryItem item = InventoryItem.builder()
                    .name(maxName)
                    .description(maxDescription)
                    .baseUnitOfMeasureId(1L)
                    .standardCost(new BigDecimal("999999999.99"))
                    .unitPerPurchaseUom(new BigDecimal("999999999.99"))
                    .reorderPointQuantity(new BigDecimal("999999999.99"))
                    .build();

            // When
            InventoryItem saved = repository.save(item);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<InventoryItem> found = repository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).hasSize(100);
            assertThat(found.get().getDescription()).hasSize(200);
        }
    }

    // ========================================
    // DELETE BY ID RETURNING COUNT
    // ========================================

    @Nested
    @DisplayName("DeleteByIdReturningCount - Optimized Delete")
    class DeleteByIdReturningCountTests {

        @Test
        @DisplayName("deleteByIdReturningCount_withExistingId_returns1AndDeletesEntity")
        void deleteByIdReturningCount_withExistingId_returns1AndDeletesEntity() {
            // Given
            InventoryItem item = createStandardItem("Item To Delete");
            Long id = item.getId();
            entityManager.clear();

            // When
            int deletedCount = repository.deleteByIdReturningCount(id);
            entityManager.flush();

            // Then
            assertThat(deletedCount).isEqualTo(1);
            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("deleteByIdReturningCount_withNonExistentId_returns0")
        void deleteByIdReturningCount_withNonExistentId_returns0() {
            // Given
            Long nonExistentId = 99999L;

            // When
            int deletedCount = repository.deleteByIdReturningCount(nonExistentId);

            // Then
            assertThat(deletedCount).isZero();
        }

        @Test
        @DisplayName("deleteByIdReturningCount_withMultipleItems_onlyDeletesTargeted")
        void deleteByIdReturningCount_withMultipleItems_onlyDeletesTargeted() {
            // Given
            InventoryItem item1 = createStandardItem("Item 1");
            InventoryItem item2 = createStandardItem("Item 2");
            InventoryItem item3 = createStandardItem("Item 3");
            Long idToDelete = item2.getId();
            entityManager.clear();

            // When
            int deletedCount = repository.deleteByIdReturningCount(idToDelete);
            entityManager.flush();

            // Then
            assertThat(deletedCount).isEqualTo(1);
            assertThat(repository.findById(item1.getId())).isPresent();
            assertThat(repository.findById(idToDelete)).isEmpty();
            assertThat(repository.findById(item3.getId())).isPresent();
            assertThat(repository.count()).isEqualTo(2);
        }
    }
}
