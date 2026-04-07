package com.elara.app.inventory_service.controller;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.exceptions.ResourceConflictException;
import com.elara.app.inventory_service.exceptions.ResourceNotFoundException;
import com.elara.app.inventory_service.service.interfaces.InventoryItemService;
import com.elara.app.inventory_service.utils.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryItemController.class)
@DisplayName("InventoryItemController REST API Tests")
class InventoryItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryItemService service;

    @MockitoBean
    private MessageService messageService;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        reset(service);
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

    private InventoryItemRequest createMaxLengthRequest() {
        String maxName = "A".repeat(100);
        String maxDescription = "B".repeat(200);
        return new InventoryItemRequest(
            maxName,
            maxDescription,
            1L,
            new BigDecimal("999.99"),
            new BigDecimal("999.99"),
            new BigDecimal("999.99")
        );
    }

    // ========================================
    // CREATE OPERATION TESTS
    // ========================================

    @Nested
    @DisplayName("Create Operation Tests - POST /item/")
    class CreateTests {

        @Test
        @DisplayName("create_withValidRequest_returnsCreated201")
        void create_withValidRequest_returnsCreated201() throws Exception {
            // Given
            InventoryItemRequest request = createStandardRequest();
            InventoryItemResponse response = createStandardResponse();

            when(service.save(any(InventoryItemRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Steel Bolt M10"))
                .andExpect(jsonPath("$.description").value("High-strength steel bolt, M10 thread"))
                .andExpect(jsonPath("$.baseUnitOfMeasureId").value(1L))
                .andExpect(jsonPath("$.standardCost").value(2.50))
                .andExpect(jsonPath("$.unitPerPurchaseUom").value(10.00))
                .andExpect(jsonPath("$.reorderPointQuantity").value(50.00));

            verify(service).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withDuplicateName_returnsConflict409")
        void create_withDuplicateName_returnsConflict409() throws Exception {
            // Given
            InventoryItemRequest request = createStandardRequest();

            when(service.save(any(InventoryItemRequest.class)))
                .thenThrow(new ResourceConflictException("InventoryItem with name 'Steel Bolt M10' already exists"));

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

            verify(service).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withInvalidData_returnsBadRequest400")
        void create_withInvalidData_returnsBadRequest400() throws Exception {
            // Given - Request with blank name and negative cost
            InventoryItemRequest invalidRequest = new InventoryItemRequest(
                "",  // Blank name
                "Description",
                1L,
                new BigDecimal("-1.00"),  // Negative cost
                new BigDecimal("-5.00"),  // Negative unitPerPurchaseUom
                new BigDecimal("-10.00")  // Negative reorderPointQuantity
            );

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

            verify(service, never()).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withMissingRequiredFields_returnsBadRequest400")
        void create_withMissingRequiredFields_returnsBadRequest400() throws Exception {
            // Given - Request with null required fields
            String incompleteJson = """
                {
                    "description": "Missing name and other required fields"
                }
                """;

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(incompleteJson))
                .andExpect(status().isBadRequest());

            verify(service, never()).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withInvalidUomId_throwsResourceNotFoundException")
        void create_withInvalidUomId_throwsResourceNotFoundException() throws Exception {
            // Given
            InventoryItemRequest request = createStandardRequest();

            when(service.save(any(InventoryItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("UOM with id '1' not found"));

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

            verify(service).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withMaxLengthFields_returnsCreated201")
        void create_withMaxLengthFields_returnsCreated201() throws Exception {
            // Given
            InventoryItemRequest request = createMaxLengthRequest();
            InventoryItemResponse response = new InventoryItemResponse(
                1L,
                request.name(),
                request.description(),
                1L,
                new BigDecimal("999.99"),
                new BigDecimal("999.99"),
                new BigDecimal("999.99")
            );

            when(service.save(any(InventoryItemRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.description").value(request.description()));

            verify(service).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withExceedingMaxLengthName_returnsBadRequest400")
        void create_withExceedingMaxLengthName_returnsBadRequest400() throws Exception {
            // Given - Name exceeds 100 characters
            InventoryItemRequest request = new InventoryItemRequest(
                "A".repeat(101),  // 101 characters
                "Valid description",
                1L,
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("50.00")
            );

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verify(service, never()).save(any(InventoryItemRequest.class));
        }

        @Test
        @DisplayName("create_withInvalidContentType_returnsUnsupportedMediaType415")
        void create_withInvalidContentType_returnsUnsupportedMediaType415() throws Exception {
            // Given
            InventoryItemRequest request = createStandardRequest();

            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_XML)  // Invalid content type
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

            verify(service, never()).save(any(InventoryItemRequest.class));
        }
    }

    // ========================================
    // GET BY ID OPERATION TESTS
    // ========================================

    @Nested
    @DisplayName("Get By ID Operation Tests - GET /item/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("getById_withExistingId_returnsOk200")
        void getById_withExistingId_returnsOk200() throws Exception {
            // Given
            Long id = 1L;
            InventoryItemResponse response = createStandardResponse();

            when(service.findById(id)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/item/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Steel Bolt M10"))
                .andExpect(jsonPath("$.standardCost").value(2.50));

            verify(service).findById(id);
        }

        @Test
        @DisplayName("getById_withNonExistentId_returnsNotFound404")
        void getById_withNonExistentId_returnsNotFound404() throws Exception {
            // Given
            Long id = 999L;

            when(service.findById(id))
                .thenThrow(new ResourceNotFoundException("InventoryItem with id '999' not found"));

            // When & Then
            mockMvc.perform(get("/item/{id}", id))
                .andExpect(status().isNotFound());

            verify(service).findById(id);
        }

        @Test
        @DisplayName("getById_withNegativeId_returnsBadRequest400")
        void getById_withNegativeId_returnsBadRequest400() throws Exception {
            // Given
            Long negativeId = -1L;

            // When & Then
            mockMvc.perform(get("/item/{id}", negativeId))
                .andExpect(status().isBadRequest());

            verify(service, never()).findById(any());
        }

        @Test
        @DisplayName("getById_withZeroId_returnsBadRequest400")
        void getById_withZeroId_returnsBadRequest400() throws Exception {
            // Given
            Long zeroId = 0L;

            // When & Then
            mockMvc.perform(get("/item/{id}", zeroId))
                .andExpect(status().isBadRequest());

            verify(service, never()).findById(any());
        }

        @Test
        @DisplayName("getById_withNonNumericId_returnsBadRequest400")
        void getById_withNonNumericId_returnsBadRequest400() throws Exception {
            // When & Then
            mockMvc.perform(get("/item/{id}", "abc"))
                .andExpect(status().isBadRequest());

            verify(service, never()).findById(any());
        }

        @Test
        @DisplayName("getById_withVeryLargeId_returnsOk200OrNotFound404")
        void getById_withVeryLargeId_returnsOk200OrNotFound404() throws Exception {
            // Given
            Long largeId = Long.MAX_VALUE;

            when(service.findById(largeId))
                .thenThrow(new ResourceNotFoundException("InventoryItem with id '" + largeId + "' not found"));

            // When & Then
            mockMvc.perform(get("/item/{id}", largeId))
                .andExpect(status().isNotFound());

            verify(service).findById(largeId);
        }
    }

    // ========================================
    // GET ALL OPERATION TESTS
    // ========================================

    @Nested
    @DisplayName("Get All Operation Tests - GET /item/")
    class GetAllTests {

        @Test
        @DisplayName("getAll_withDefaultPagination_returnsOk200")
        void getAll_withDefaultPagination_returnsOk200() throws Exception {
            // Given
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

            Page<InventoryItemResponse> page = new PageImpl<>(
                List.of(response1, response2),
                PageRequest.of(0, 20),
                2
            );

            when(service.findAll(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/item/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Steel Bolt M10"))
                .andExpect(jsonPath("$.content[1].name").value("Copper Wire 2mm"))
                .andExpect(jsonPath("$.page", allOf(
                        hasEntry("totalElements", 2),
                        hasEntry("totalPages", 1),
                        hasEntry("size", 20),
                        hasEntry("number", 0)
                )));

            verify(service).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getAll_withCustomPagination_returnsOk200")
        void getAll_withCustomPagination_returnsOk200() throws Exception {
            // Given
            Page<InventoryItemResponse> page = new PageImpl<>(
                List.of(createStandardResponse()),
                PageRequest.of(1, 5),
                10
            );

            when(service.findAll(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/item/")
                    .param("page", "1")
                    .param("size", "5")
                    .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page", allOf(
                        hasEntry("totalElements", 10),
                        hasEntry("size", 5),
                        hasEntry("number", 1),
                        hasEntry("totalPages", 2)
                )));

            verify(service).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getAll_withEmptyResult_returnsOk200WithEmptyPage")
        void getAll_withEmptyResult_returnsOk200WithEmptyPage() throws Exception {
            // Given
            Page<InventoryItemResponse> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
            );

            when(service.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/item/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page", allOf(
                        hasEntry("totalElements", 0),
                        hasEntry("totalPages", 0)
                )));

            verify(service).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getAll_withInvalidSortField_returnsOk200WithDefaultSort")
        void getAll_withInvalidSortField_returnsOk200WithDefaultSort() throws Exception {
            // Given
            Page<InventoryItemResponse> page = new PageImpl<>(
                List.of(createStandardResponse()),
                PageRequest.of(0, 20),
                1
            );

            when(service.findAll(any(Pageable.class))).thenReturn(page);

            // When & Then - Spring will handle invalid sort gracefully
            mockMvc.perform(get("/item/")
                    .param("sort", "invalidField,asc"))
                .andExpect(status().isOk());

            verify(service).findAll(any(Pageable.class));
        }
    }

    // ========================================
    // SEARCH BY NAME OPERATION TESTS
    // ========================================

    @Nested
    @DisplayName("Search By Name Operation Tests - GET /item/search")
    class SearchByNameTests {

        @Test
        @DisplayName("search_withValidName_returnsOk200")
        void search_withValidName_returnsOk200() throws Exception {
            // Given
            String searchName = "bolt";
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

            Page<InventoryItemResponse> page = new PageImpl<>(
                List.of(response1, response2),
                PageRequest.of(0, 20),
                2
            );

            when(service.findAllByName(eq(searchName), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/item/search")
                    .param("name", searchName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value(containsStringIgnoringCase("bolt")))
                .andExpect(jsonPath("$.content[1].name").value(containsStringIgnoringCase("bolt")))
                .andExpect(jsonPath("$.page.totalElements").value(2));

            verify(service).findAllByName(eq(searchName), any(Pageable.class));
        }

        @Test
        @DisplayName("search_withNoMatches_returnsOk200WithEmptyPage")
        void search_withNoMatches_returnsOk200WithEmptyPage() throws Exception {
            // Given
            String searchName = "nonexistent";
            Page<InventoryItemResponse> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
            );

            when(service.findAllByName(eq(searchName), any(Pageable.class))).thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/item/search")
                    .param("name", searchName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));

            verify(service).findAllByName(eq(searchName), any(Pageable.class));
        }

        @Test
        @DisplayName("search_withBlankName_returnsBadRequest400")
        void search_withBlankName_returnsBadRequest400() throws Exception {
            // When & Then
            mockMvc.perform(get("/item/search")
                    .param("name", "   "))  // Blank name
                .andExpect(status().isBadRequest());

            verify(service, never()).findAllByName(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("search_withPagination_appliesPaginationCorrectly")
        void search_withPagination_appliesPaginationCorrectly() throws Exception {
            // Given
            String searchName = "steel";
            Page<InventoryItemResponse> page = new PageImpl<>(
                List.of(createStandardResponse()),
                PageRequest.of(1, 10),
                25
            );

            when(service.findAllByName(eq(searchName), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/item/search")
                    .param("name", searchName)
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page", allOf(
                        hasEntry("totalElements", 25),
                        hasEntry("size", 10),
                        hasEntry("number", 1),
                        hasEntry("totalPages", 3)
                )));

            verify(service).findAllByName(eq(searchName), any(Pageable.class));
        }

        @Test
        @DisplayName("search_withMissingNameParameter_returnsBadRequest400")
        void search_withMissingNameParameter_returnsBadRequest400() throws Exception {
            // When & Then
            mockMvc.perform(get("/item/search"))  // Missing 'name' parameter
                .andExpect(status().isBadRequest());

            verify(service, never()).findAllByName(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("search_withCaseInsensitiveMatch_returnsOk200")
        void search_withCaseInsensitiveMatch_returnsOk200() throws Exception {
            // Given
            String searchName = "STEEL";
            Page<InventoryItemResponse> page = new PageImpl<>(
                List.of(createStandardResponse()),
                PageRequest.of(0, 20),
                1
            );

            when(service.findAllByName(eq(searchName), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/item/search")
                    .param("name", searchName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Steel Bolt M10"));

            verify(service).findAllByName(eq(searchName), any(Pageable.class));
        }
    }

    // ========================================
    // UPDATE OPERATION TESTS
    // ========================================

    @Nested
    @DisplayName("Update Operation Tests - PUT /item/{id}")
    class UpdateTests {

        @Test
        @DisplayName("update_withValidData_returnsOk200")
        void update_withValidData_returnsOk200() throws Exception {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = createStandardUpdate();
            InventoryItemResponse response = new InventoryItemResponse(
                1L,
                "Steel Bolt M10 Updated",
                "Updated description",
                2L,
                new BigDecimal("3.00"),
                new BigDecimal("12.00"),
                new BigDecimal("60.00")
            );

            when(service.update(eq(id), any(InventoryItemUpdate.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Steel Bolt M10 Updated"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.baseUnitOfMeasureId").value(2L))
                .andExpect(jsonPath("$.standardCost").value(3.00))
                .andExpect(jsonPath("$.unitPerPurchaseUom").value(12.00))
                .andExpect(jsonPath("$.reorderPointQuantity").value(60.00));

            verify(service).update(eq(id), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withNonExistentId_returnsNotFound404")
        void update_withNonExistentId_returnsNotFound404() throws Exception {
            // Given
            Long id = 999L;
            InventoryItemUpdate update = createStandardUpdate();

            when(service.update(eq(id), any(InventoryItemUpdate.class)))
                .thenThrow(new ResourceNotFoundException("InventoryItem with id '999' not found"));

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

            verify(service).update(eq(id), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withDuplicateName_returnsConflict409")
        void update_withDuplicateName_returnsConflict409() throws Exception {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = createStandardUpdate();

            when(service.update(eq(id), any(InventoryItemUpdate.class)))
                .thenThrow(new ResourceConflictException("InventoryItem with name 'Steel Bolt M10 Updated' already exists"));

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict());

            verify(service).update(eq(id), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withInvalidData_returnsBadRequest400")
        void update_withInvalidData_returnsBadRequest400() throws Exception {
            // Given
            Long id = 1L;
            InventoryItemUpdate invalidUpdate = new InventoryItemUpdate(
                "",  // Blank name
                "Description",
                1L,
                new BigDecimal("-1.00"),  // Negative cost
                new BigDecimal("-5.00"),
                new BigDecimal("-10.00")
            );

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest());

            verify(service, never()).update(any(), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withInvalidIdFormat_returnsBadRequest400")
        void update_withInvalidIdFormat_returnsBadRequest400() throws Exception {
            // Given
            Long negativeId = -1L;
            InventoryItemUpdate update = createStandardUpdate();

            // When & Then
            mockMvc.perform(put("/item/{id}", negativeId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());

            verify(service, never()).update(any(), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withMissingFields_returnsBadRequest400")
        void update_withMissingFields_returnsBadRequest400() throws Exception {
            // Given
            Long id = 1L;
            String incompleteJson = """
                {
                    "name": "Updated Name"
                }
                """;

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(incompleteJson))
                .andExpect(status().isBadRequest());

            verify(service, never()).update(any(), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withExceedingMaxLength_returnsBadRequest400")
        void update_withExceedingMaxLength_returnsBadRequest400() throws Exception {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = new InventoryItemUpdate(
                "A".repeat(101),  // Exceeds max length
                "Description",
                1L,
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("50.00")
            );

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());

            verify(service, never()).update(any(), any(InventoryItemUpdate.class));
        }

        @Test
        @DisplayName("update_withInvalidUomId_throwsResourceNotFoundException")
        void update_withInvalidUomId_throwsResourceNotFoundException() throws Exception {
            // Given
            Long id = 1L;
            InventoryItemUpdate update = createStandardUpdate();

            when(service.update(eq(id), any(InventoryItemUpdate.class)))
                .thenThrow(new ResourceNotFoundException("UOM with id '2' not found"));

            // When & Then
            mockMvc.perform(put("/item/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

            verify(service).update(eq(id), any(InventoryItemUpdate.class));
        }
    }

    // ========================================
    // DELETE OPERATION TESTS
    // ========================================

    @Nested
    @DisplayName("Delete Operation Tests - DELETE /item/{id}")
    class DeleteTests {

        @Test
        @DisplayName("delete_withExistingId_returnsNoContent204")
        void delete_withExistingId_returnsNoContent204() throws Exception {
            // Given
            Long id = 1L;

            doNothing().when(service).deleteById(id);

            // When & Then
            mockMvc.perform(delete("/item/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));  // Empty body

            verify(service).deleteById(id);
        }

        @Test
        @DisplayName("delete_withNonExistentId_returnsNotFound404")
        void delete_withNonExistentId_returnsNotFound404() throws Exception {
            // Given
            Long id = 999L;

            doThrow(new ResourceNotFoundException("InventoryItem with id '999' not found"))
                .when(service).deleteById(id);

            // When & Then
            mockMvc.perform(delete("/item/{id}", id))
                .andExpect(status().isNotFound());

            verify(service).deleteById(id);
        }

        @Test
        @DisplayName("delete_withInvalidIdFormat_returnsBadRequest400")
        void delete_withInvalidIdFormat_returnsBadRequest400() throws Exception {
            // Given
            Long negativeId = -1L;

            // When & Then
            mockMvc.perform(delete("/item/{id}", negativeId))
                .andExpect(status().isBadRequest());

            verify(service, never()).deleteById(any());
        }

        @Test
        @DisplayName("delete_withZeroId_returnsBadRequest400")
        void delete_withZeroId_returnsBadRequest400() throws Exception {
            // Given
            Long zeroId = 0L;

            // When & Then
            mockMvc.perform(delete("/item/{id}", zeroId))
                .andExpect(status().isBadRequest());

            verify(service, never()).deleteById(any());
        }

        @Test
        @DisplayName("delete_withNonNumericId_returnsBadRequest400")
        void delete_withNonNumericId_returnsBadRequest400() throws Exception {
            // When & Then
            mockMvc.perform(delete("/item/{id}", "abc"))
                .andExpect(status().isBadRequest());

            verify(service, never()).deleteById(any());
        }
    }
}
