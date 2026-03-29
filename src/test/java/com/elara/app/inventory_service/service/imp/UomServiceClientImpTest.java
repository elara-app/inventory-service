package com.elara.app.inventory_service.service.imp;

import com.elara.app.inventory_service.dto.response.UomResponse;
import com.elara.app.inventory_service.exceptions.ResourceNotFoundException;
import com.elara.app.inventory_service.utils.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UomServiceClientImp Tests")
class UomServiceClientImpTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private UomServiceClientImp uomServiceClient;

    @AfterEach
    void tearDown() {
        reset(restTemplate, messageService);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private UomResponse createStandardUomResponse() {
        return new UomResponse(
            1L,
            "Each",
            "Individual unit",
            new BigDecimal("1.00"),
            1L
        );
    }

    private void setUomServiceName(String serviceName) {
        ReflectionTestUtils.setField(uomServiceClient, "uomServiceName", serviceName);
    }

    // ========================================
    // VERIFY UOM BY ID TESTS
    // ========================================

    @Nested
    @DisplayName("Verify UOM By ID Operation Tests")
    class VerifyUomByIdTests {

        @Test
        @DisplayName("verifyUomById_withValidId_completesSuccessfully")
        void verifyUomById_withValidId_completesSuccessfully() {
            // Given
            Long uomId = 1L;
            UomResponse uomResponse = createStandardUomResponse();
            ResponseEntity<UomResponse> responseEntity = ResponseEntity.ok(uomResponse);

            setUomServiceName("unit-of-measure-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenReturn(responseEntity);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Success message");

            // When & Then
            assertThatCode(() -> uomServiceClient.verifyUomById(uomId))
                .doesNotThrowAnyException();

            verify(restTemplate).getForEntity(any(URI.class), eq(UomResponse.class));
            verify(messageService).getMessage("crud.read.success", "Uom");
        }

        @Test
        @DisplayName("verifyUomById_withNonExistentId_throwsResourceNotFoundException")
        void verifyUomById_withNonExistentId_throwsResourceNotFoundException() {
            // Given
            Long uomId = 999L;
            String errorMessage = "UOM with id '999' not found";

            setUomServiceName("unit-of-measure-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    null,
                    null,
                    null
                ));
            when(messageService.getMessage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(errorMessage);

            // When & Then
            assertThatThrownBy(() -> uomServiceClient.verifyUomById(uomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(restTemplate).getForEntity(any(URI.class), eq(UomResponse.class));
            verify(messageService).getMessage("crud.not.found", "UOM", "id", uomId.toString());
            verify(messageService, never()).getMessage(eq("crud.read.success"), anyString());
        }

        @Test
        @DisplayName("verifyUomById_withDifferentServiceName_constructsCorrectUrl")
        void verifyUomById_withDifferentServiceName_constructsCorrectUrl() {
            // Given
            Long uomId = 2L;
            UomResponse uomResponse = new UomResponse(
                2L,
                "Kilogram",
                "Weight measurement unit",
                new BigDecimal("1000.00"),
                1L
            );
            ResponseEntity<UomResponse> responseEntity = ResponseEntity.ok(uomResponse);

            setUomServiceName("custom-uom-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenReturn(responseEntity);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Success message");

            // When & Then
            assertThatCode(() -> uomServiceClient.verifyUomById(uomId))
                .doesNotThrowAnyException();

            verify(restTemplate).getForEntity(argThat(uri ->
                uri.toString().contains("custom-uom-service") &&
                uri.toString().contains("/" + uomId)
            ), eq(UomResponse.class));
        }

        @Test
        @DisplayName("verifyUomById_withMultipleValidIds_successfullyVerifiesAll")
        void verifyUomById_withMultipleValidIds_successfullyVerifiesAll() {
            // Given
            Long uomId1 = 1L;
            Long uomId2 = 2L;
            Long uomId3 = 3L;

            UomResponse response1 = createStandardUomResponse();
            UomResponse response2 = new UomResponse(2L, "Kilogram", "Weight unit", new BigDecimal("1000.00"), 1L);
            UomResponse response3 = new UomResponse(3L, "Meter", "Length unit", new BigDecimal("100.00"), 1L);

            setUomServiceName("unit-of-measure-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenReturn(ResponseEntity.ok(response1))
                .thenReturn(ResponseEntity.ok(response2))
                .thenReturn(ResponseEntity.ok(response3));
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Success message");

            // When & Then
            assertThatCode(() -> {
                uomServiceClient.verifyUomById(uomId1);
                uomServiceClient.verifyUomById(uomId2);
                uomServiceClient.verifyUomById(uomId3);
            }).doesNotThrowAnyException();

            verify(restTemplate, times(3)).getForEntity(any(URI.class), eq(UomResponse.class));
            verify(messageService, times(3)).getMessage("crud.read.success", "Uom");
        }

        @Test
        @DisplayName("verifyUomById_withServiceUnavailable_throwsException")
        void verifyUomById_withServiceUnavailable_throwsException() {
            // Given
            Long uomId = 1L;

            setUomServiceName("unit-of-measure-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

            // When & Then
            assertThatThrownBy(() -> uomServiceClient.verifyUomById(uomId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service unavailable");

            verify(restTemplate).getForEntity(any(URI.class), eq(UomResponse.class));
            verify(messageService, never()).getMessage(anyString(), anyString());
        }

        @Test
        @DisplayName("verifyUomById_with404NotFound_throwsResourceNotFoundException")
        void verifyUomById_with404NotFound_throwsResourceNotFoundException() {
            // Given
            Long uomId = 777L;
            String errorMessage = "UOM with id '777' not found";

            setUomServiceName("unit-of-measure-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    null,
                    null,
                    null
                ));
            when(messageService.getMessage(eq("crud.not.found"), eq("UOM"), eq("id"), eq(uomId.toString())))
                .thenReturn(errorMessage);

            // When & Then
            assertThatThrownBy(() -> uomServiceClient.verifyUomById(uomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(errorMessage);

            verify(restTemplate).getForEntity(any(URI.class), eq(UomResponse.class));
            verify(messageService).getMessage("crud.not.found", "UOM", "id", uomId.toString());
        }

        @Test
        @DisplayName("verifyUomById_withDefaultServiceName_usesCorrectDefault")
        void verifyUomById_withDefaultServiceName_usesCorrectDefault() {
            // Given
            Long uomId = 5L;
            UomResponse uomResponse = new UomResponse(
                5L,
                "Liter",
                "Volume unit",
                new BigDecimal("1000.00"),
                1L
            );
            ResponseEntity<UomResponse> responseEntity = ResponseEntity.ok(uomResponse);

            // Don't set custom service name - use default from @Value annotation
            setUomServiceName("unit-of-measure-service");
            when(restTemplate.getForEntity(any(URI.class), eq(UomResponse.class)))
                .thenReturn(responseEntity);
            when(messageService.getMessage(anyString(), anyString())).thenReturn("Success message");

            // When & Then
            assertThatCode(() -> uomServiceClient.verifyUomById(uomId))
                .doesNotThrowAnyException();

            verify(restTemplate).getForEntity(argThat(uri ->
                uri.toString().startsWith("http://unit-of-measure-service/")
            ), eq(UomResponse.class));
        }
    }
}
