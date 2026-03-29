package com.elara.app.inventory_service.service.imp;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.exceptions.ResourceConflictException;
import com.elara.app.inventory_service.exceptions.ResourceNotFoundException;
import com.elara.app.inventory_service.mapper.InventoryItemMapper;
import com.elara.app.inventory_service.model.InventoryItem;
import com.elara.app.inventory_service.repository.InventoryItemRepository;
import com.elara.app.inventory_service.service.interfaces.UomServiceClient;
import com.elara.app.inventory_service.utils.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryItemImp Service Tests")
class InventoryItemImpTest {

    @Mock
    private InventoryItemMapper mapper;

    @Mock
    private InventoryItemRepository repository;

    @Mock
    private MessageService messageService;

    @Mock
    private UomServiceClient uomServiceClient;

    @InjectMocks
    private InventoryItemImp service;

    @AfterEach
    void tearDown() {
        reset(mapper, repository, messageService, uomServiceClient);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private InventoryItemRequest createStandardRequest() {
        return new InventoryItemRequest(
            "Steel Bolt M10",
            "High-strength steel bolt, M10 thread",
            1L,
            new BigDecimal("2.50"),
            new BigDecimal("10.00"),
            new BigDecimal("50.00")
        );
    }

    private InventoryItemUpdate createStandardUpdate() {
        return new InventoryItemUpdate(
            "Steel Bolt M10 Updated",
            "Updated description",
            2L,
            new BigDecimal("3.00"),
            new BigDecimal("12.00"),
            new BigDecimal("60.00")
        );
    }

    private InventoryItem createStandardEntity() {
        return InventoryItem.builder()
            .id(1L)
            .name("Steel Bolt M10")
            .description("High-strength steel bolt, M10 thread")
            .baseUnitOfMeasureId(1L)
            .standardCost(new BigDecimal("2.50"))
            .unitPerPurchaseUom(new BigDecimal("10.00"))
            .reorderPointQuantity(new BigDecimal("50.00"))
            .build();
    }

    private InventoryItemResponse createStandardResponse() {
        return new InventoryItemResponse(
            1L,
            "Steel Bolt M10",
            "High-strength steel bolt, M10 thread",
            1L,
            new BigDecimal("2.50"),
            new BigDecimal("10.00"),
            new BigDecimal("50.00")
        );
    }

    // ========================================
    // SAVE TESTS
    // ========================================

    @Nested
    @DisplayName("Save Operation Tests")
    class SaveTests {

        @Test
        @DisplayName("save_withValidRequest_createsAndReturnsResponse")
        void save_withValidRequest_createsAndReturnsResponse() {
            // Given
            InventoryItemRequest request = createStandardRequest();
            InventoryItem entity = createStandardEntity();
            InventoryItemResponse expectedResponse = createStandardResponse();

            when(repository.existsByNameIgnoreCase(request.name())).thenReturn(false);
            doNothing().when(uomServiceClient).verifyUomById(request.baseUnitOfMeasureId());
            when(mapper.toEntity(request)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(expectedResponse);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Success message");

            // When
            InventoryItemResponse result = service.save(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Steel Bolt M10");
            assertThat(result.standardCost()).isEqualByComparingTo(new BigDecimal("2.50"));

            verify(repository).existsByNameIgnoreCase(request.name());
            verify(uomServiceClient).verifyUomById(request.baseUnitOfMeasureId());
            verify(mapper).toEntity(request);
            verify(repository).save(entity);
            verify(mapper).toResponse(entity);
        }

        @Test
        @DisplayName("save_withDuplicateName_throwsResourceConflictException")
        void save_withDuplicateName_throwsResourceConflictException() {
            // Given
            InventoryItemRequest request = createStandardRequest();
            String errorMessage = "InventoryItem with name 'Steel Bolt M10' already exists";

            when(repository.existsByNameIgnoreCase(request.name())).thenReturn(true);
            when(messageService.getMessage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(errorMessage);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Save error");

            // When & Then
            assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(errorMessage);

            verify(repository).existsByNameIgnoreCase(request.name());
            verify(uomServiceClient, never()).verifyUomById(anyLong());
            verify(mapper, never()).toEntity(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("save_withInvalidUomId_throwsResourceNotFoundException")
        void save_withInvalidUomId_throwsResourceNotFoundException() {
            // Given
            InventoryItemRequest request = createStandardRequest();
            String errorMessage = "UOM with id '1' not found";

            when(repository.existsByNameIgnoreCase(request.name())).thenReturn(false);
            doThrow(new ResourceNotFoundException(errorMessage))
                .when(uomServiceClient).verifyUomById(request.baseUnitOfMeasureId());

            // When & Then
            assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(repository).existsByNameIgnoreCase(request.name());
            verify(uomServiceClient).verifyUomById(request.baseUnitOfMeasureId());
            verify(mapper, never()).toEntity(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("save_withNullRequest_throwsNullPointerException")
        void save_withNullRequest_throwsNullPointerException() {
            // When & Then
            assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(NullPointerException.class);

            verify(repository, never()).existsByNameIgnoreCase(anyString());
            verify(uomServiceClient, never()).verifyUomById(anyLong());
            verify(repository, never()).save(any());
        }
    }

    // ========================================
    // UPDATE TESTS
    // ========================================

    @Nested
    @DisplayName("Update Operation Tests")
    class UpdateTests {

        @Test
        @DisplayName("update_withValidData_updatesAndReturnsResponse")
        void update_withValidData_updatesAndReturnsResponse() {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = createStandardUpdate();
            InventoryItem existingEntity = createStandardEntity();
            InventoryItemResponse expectedResponse = new InventoryItemResponse(
                1L,
                "Steel Bolt M10 Updated",
                "Updated description",
                2L,
                new BigDecimal("3.00"),
                new BigDecimal("12.00"),
                new BigDecimal("60.00")
            );

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            when(repository.existsByNameIgnoreCase(update.name())).thenReturn(false);
            doNothing().when(uomServiceClient).verifyUomById(update.baseUnitOfMeasureId());
            doNothing().when(mapper).updateEntityFromDto(existingEntity, update);
            when(mapper.toResponse(existingEntity)).thenReturn(expectedResponse);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Update success");

            // When
            InventoryItemResponse result = service.update(id, update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Steel Bolt M10 Updated");
            assertThat(result.standardCost()).isEqualByComparingTo(new BigDecimal("3.00"));

            verify(repository).findById(id);
            verify(repository).existsByNameIgnoreCase(update.name());
            verify(uomServiceClient).verifyUomById(update.baseUnitOfMeasureId());
            verify(mapper).updateEntityFromDto(existingEntity, update);
            verify(mapper).toResponse(existingEntity);
        }

        @Test
        @DisplayName("update_withSameName_updatesWithoutNameCheck")
        void update_withSameName_updatesWithoutNameCheck() {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = new InventoryItemUpdate(
                "Steel Bolt M10", // Same name as existing
                "Updated description",
                2L,
                new BigDecimal("3.00"),
                new BigDecimal("12.00"),
                new BigDecimal("60.00")
            );
            InventoryItem existingEntity = createStandardEntity();
            InventoryItemResponse expectedResponse = new InventoryItemResponse(
                1L,
                "Steel Bolt M10",
                "Updated description",
                2L,
                new BigDecimal("3.00"),
                new BigDecimal("12.00"),
                new BigDecimal("60.00")
            );

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            doNothing().when(uomServiceClient).verifyUomById(update.baseUnitOfMeasureId());
            doNothing().when(mapper).updateEntityFromDto(existingEntity, update);
            when(mapper.toResponse(existingEntity)).thenReturn(expectedResponse);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Update success");

            // When
            InventoryItemResponse result = service.update(id, update);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Steel Bolt M10");

            verify(repository).findById(id);
            verify(repository, never()).existsByNameIgnoreCase(anyString());
            verify(uomServiceClient).verifyUomById(update.baseUnitOfMeasureId());
            verify(mapper).updateEntityFromDto(existingEntity, update);
        }

        @Test
        @DisplayName("update_withNonExistentId_throwsResourceNotFoundException")
        void update_withNonExistentId_throwsResourceNotFoundException() {
            // Given
            Long id = 999L;
            InventoryItemUpdate update = createStandardUpdate();
            String errorMessage = "InventoryItem with id '999' not found";

            when(repository.findById(id)).thenReturn(Optional.empty());
            when(messageService.getMessage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(errorMessage);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Update error");

            // When & Then
            assertThatThrownBy(() -> service.update(id, update))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(repository).findById(id);
            verify(repository, never()).existsByNameIgnoreCase(anyString());
            verify(uomServiceClient, never()).verifyUomById(anyLong());
            verify(mapper, never()).updateEntityFromDto(any(), any());
        }

        @Test
        @DisplayName("update_withDuplicateName_throwsResourceConflictException")
        void update_withDuplicateName_throwsResourceConflictException() {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = createStandardUpdate();
            InventoryItem existingEntity = createStandardEntity();
            String errorMessage = "InventoryItem with name 'Steel Bolt M10 Updated' already exists";

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            when(repository.existsByNameIgnoreCase(update.name())).thenReturn(true);
            when(messageService.getMessage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(errorMessage);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Update error");

            // When & Then
            assertThatThrownBy(() -> service.update(id, update))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(errorMessage);

            verify(repository).findById(id);
            verify(repository).existsByNameIgnoreCase(update.name());
            verify(uomServiceClient, never()).verifyUomById(anyLong());
            verify(mapper, never()).updateEntityFromDto(any(), any());
        }

        @Test
        @DisplayName("update_withInvalidUomId_throwsResourceNotFoundException")
        void update_withInvalidUomId_throwsResourceNotFoundException() {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = createStandardUpdate();
            InventoryItem existingEntity = createStandardEntity();
            String errorMessage = "UOM with id '2' not found";

            when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
            when(repository.existsByNameIgnoreCase(update.name())).thenReturn(false);
            doThrow(new ResourceNotFoundException(errorMessage))
                .when(uomServiceClient).verifyUomById(update.baseUnitOfMeasureId());
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Update error");

            // When & Then
            assertThatThrownBy(() -> service.update(id, update))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(repository).findById(id);
            verify(repository).existsByNameIgnoreCase(update.name());
            verify(uomServiceClient).verifyUomById(update.baseUnitOfMeasureId());
            verify(mapper, never()).updateEntityFromDto(any(), any());
        }
    }

    // ========================================
    // DELETE TESTS
    // ========================================

    @Nested
    @DisplayName("Delete Operation Tests")
    class DeleteTests {

        @Test
        @DisplayName("deleteById_withExistingId_deletesSuccessfully")
        void deleteById_withExistingId_deletesSuccessfully() {
            // Given
            Long id = 1L;

            when(repository.existsById(id)).thenReturn(true);
            doNothing().when(repository).deleteById(id);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Delete success");

            // When
            service.deleteById(id);

            // Then
            verify(repository).existsById(id);
            verify(repository).deleteById(id);
        }

        @Test
        @DisplayName("deleteById_withNonExistentId_throwsResourceNotFoundException")
        void deleteById_withNonExistentId_throwsResourceNotFoundException() {
            // Given
            Long id = 999L;
            String errorMessage = "InventoryItem with id '999' not found";

            when(repository.existsById(id)).thenReturn(false);
            when(messageService.getMessage(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(errorMessage);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Delete error");

            // When & Then
            assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(repository).existsById(id);
            verify(repository, never()).deleteById(anyLong());
        }
    }

    // ========================================
    // FIND BY ID TESTS
    // ========================================

    @Nested
    @DisplayName("Find By ID Operation Tests")
    class FindByIdTests {

        @Test
        @DisplayName("findById_withExistingId_returnsResponse")
        void findById_withExistingId_returnsResponse() {
            // Given
            Long id = 1L;
            InventoryItem entity = createStandardEntity();
            InventoryItemResponse expectedResponse = createStandardResponse();

            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(mapper.toResponse(entity)).thenReturn(expectedResponse);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Read success");

            // When
            InventoryItemResponse result = service.findById(id);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Steel Bolt M10");
            assertThat(result.standardCost()).isEqualByComparingTo(new BigDecimal("2.50"));

            verify(repository).findById(id);
            verify(mapper).toResponse(entity);
        }

        @Test
        @DisplayName("findById_withNonExistentId_throwsResourceNotFoundException")
        void findById_withNonExistentId_throwsResourceNotFoundException() {
            // Given
            Long id = 999L;
            String errorMessage = "InventoryItem with id '999' not found";

            when(repository.findById(id)).thenReturn(Optional.empty());
            when(messageService.getMessage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(errorMessage);

            // When & Then
            assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(repository).findById(id);
            verify(mapper, never()).toResponse(any());
        }
    }

    // ========================================
    // FIND ALL TESTS
    // ========================================

    @Nested
    @DisplayName("Find All Operation Tests")
    class FindAllTests {

        @Test
        @DisplayName("findAll_withData_returnsPagedResponse")
        void findAll_withData_returnsPagedResponse() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            InventoryItem entity1 = createStandardEntity();
            InventoryItem entity2 = InventoryItem.builder()
                .id(2L)
                .name("Copper Wire 2mm")
                .description("Electrical copper wire")
                .baseUnitOfMeasureId(2L)
                .standardCost(new BigDecimal("5.75"))
                .unitPerPurchaseUom(new BigDecimal("100.00"))
                .reorderPointQuantity(new BigDecimal("200.00"))
                .build();

            InventoryItemResponse response1 = createStandardResponse();
            InventoryItemResponse response2 = new InventoryItemResponse(
                2L,
                "Copper Wire 2mm",
                "Electrical copper wire",
                2L,
                new BigDecimal("5.75"),
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
            );

            Page<InventoryItem> entityPage = new PageImpl<>(List.of(entity1, entity2), pageable, 2);

            when(repository.findAll(pageable)).thenReturn(entityPage);
            when(mapper.toResponse(entity1)).thenReturn(response1);
            when(mapper.toResponse(entity2)).thenReturn(response2);

            // When
            Page<InventoryItemResponse> result = service.findAll(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).name()).isEqualTo("Steel Bolt M10");
            assertThat(result.getContent().get(1).name()).isEqualTo("Copper Wire 2mm");

            verify(repository).findAll(pageable);
            verify(mapper, times(2)).toResponse(any(InventoryItem.class));
        }

        @Test
        @DisplayName("findAll_withEmptyDatabase_returnsEmptyPage")
        void findAll_withEmptyDatabase_returnsEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<InventoryItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(repository.findAll(pageable)).thenReturn(emptyPage);

            // When
            Page<InventoryItemResponse> result = service.findAll(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(repository).findAll(pageable);
            verify(mapper, never()).toResponse(any());
        }
    }

    // ========================================
    // FIND ALL BY NAME TESTS
    // ========================================

    @Nested
    @DisplayName("Find All By Name Operation Tests")
    class FindAllByNameTests {

        @Test
        @DisplayName("findAllByName_withMatchingResults_returnsPagedResponse")
        void findAllByName_withMatchingResults_returnsPagedResponse() {
            // Given
            String searchName = "bolt";
            Pageable pageable = PageRequest.of(0, 10);
            InventoryItem entity1 = createStandardEntity();
            InventoryItem entity2 = InventoryItem.builder()
                .id(2L)
                .name("Steel Bolt M12")
                .description("Heavy-duty steel bolt")
                .baseUnitOfMeasureId(1L)
                .standardCost(new BigDecimal("3.25"))
                .unitPerPurchaseUom(new BigDecimal("8.00"))
                .reorderPointQuantity(new BigDecimal("40.00"))
                .build();

            InventoryItemResponse response1 = createStandardResponse();
            InventoryItemResponse response2 = new InventoryItemResponse(
                2L,
                "Steel Bolt M12",
                "Heavy-duty steel bolt",
                1L,
                new BigDecimal("3.25"),
                new BigDecimal("8.00"),
                new BigDecimal("40.00")
            );

            Page<InventoryItem> entityPage = new PageImpl<>(List.of(entity1, entity2), pageable, 2);

            when(repository.findAllByNameContainingIgnoreCase(searchName, pageable)).thenReturn(entityPage);
            when(mapper.toResponse(entity1)).thenReturn(response1);
            when(mapper.toResponse(entity2)).thenReturn(response2);

            // When
            Page<InventoryItemResponse> result = service.findAllByName(searchName, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).name()).contains("Bolt");
            assertThat(result.getContent().get(1).name()).contains("Bolt");

            verify(repository).findAllByNameContainingIgnoreCase(searchName, pageable);
            verify(mapper, times(2)).toResponse(any(InventoryItem.class));
        }

        @Test
        @DisplayName("findAllByName_withNoMatches_returnsEmptyPage")
        void findAllByName_withNoMatches_returnsEmptyPage() {
            // Given
            String searchName = "nonexistent";
            Pageable pageable = PageRequest.of(0, 10);
            Page<InventoryItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(repository.findAllByNameContainingIgnoreCase(searchName, pageable)).thenReturn(emptyPage);

            // When
            Page<InventoryItemResponse> result = service.findAllByName(searchName, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(repository).findAllByNameContainingIgnoreCase(searchName, pageable);
            verify(mapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("findAllByName_caseInsensitive_returnsMatches")
        void findAllByName_caseInsensitive_returnsMatches() {
            // Given
            String searchName = "STEEL";
            Pageable pageable = PageRequest.of(0, 10);
            InventoryItem entity = createStandardEntity();
            InventoryItemResponse response = createStandardResponse();

            Page<InventoryItem> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

            when(repository.findAllByNameContainingIgnoreCase(searchName, pageable)).thenReturn(entityPage);
            when(mapper.toResponse(entity)).thenReturn(response);

            // When
            Page<InventoryItemResponse> result = service.findAllByName(searchName, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().name()).isEqualTo("Steel Bolt M10");

            verify(repository).findAllByNameContainingIgnoreCase(searchName, pageable);
            verify(mapper).toResponse(entity);
        }
    }

    // ========================================
    // IS NAME TAKEN TESTS
    // ========================================

    @Nested
    @DisplayName("Is Name Taken Operation Tests")
    class IsNameTakenTests {

        @Test
        @DisplayName("isNameTaken_withExistingName_returnsTrue")
        void isNameTaken_withExistingName_returnsTrue() {
            // Given
            String name = "Steel Bolt M10";

            when(repository.existsByNameIgnoreCase(name)).thenReturn(true);

            // When
            Boolean result = service.isNameTaken(name);

            // Then
            assertThat(result).isTrue();

            verify(repository).existsByNameIgnoreCase(name);
        }

        @Test
        @DisplayName("isNameTaken_withNonExistentName_returnsFalse")
        void isNameTaken_withNonExistentName_returnsFalse() {
            // Given
            String name = "Nonexistent Item";

            when(repository.existsByNameIgnoreCase(name)).thenReturn(false);

            // When
            Boolean result = service.isNameTaken(name);

            // Then
            assertThat(result).isFalse();

            verify(repository).existsByNameIgnoreCase(name);
        }

        @Test
        @DisplayName("isNameTaken_caseInsensitive_returnsTrue")
        void isNameTaken_caseInsensitive_returnsTrue() {
            // Given
            String name = "STEEL BOLT M10";

            when(repository.existsByNameIgnoreCase(name)).thenReturn(true);

            // When
            Boolean result = service.isNameTaken(name);

            // Then
            assertThat(result).isTrue();

            verify(repository).existsByNameIgnoreCase(name);
        }
    }
}
