package com.elara.app.inventory_service.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService")
class MessageServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MessageService messageService;

    @AfterEach
    void tearDown() {
        reset(messageSource);
    }

    // ========================================
    // GET MESSAGE BY KEY TESTS
    // ========================================

    @Nested
    @DisplayName("Get Message By Key Tests")
    class GetMessageByKeyTests {

        @Test
        @DisplayName("getMessage_withValidKey_returnsMessage")
        void getMessage_withValidKey_returnsMessage() {
            // Given
            String key = "inventoryItem.invalid.data";
            String expectedMessage = "Invalid inventory item data";

            doReturn(expectedMessage)
                .when(messageSource).getMessage(eq(key), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(key);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("getMessage_withInvalidKey_returnsFallbackMessage")
        void getMessage_withInvalidKey_returnsFallbackMessage() {
            // Given
            String invalidKey = "nonexistent.key";
            String fallbackMessage = "An unexpected error occurred";

            doThrow(new NoSuchMessageException(invalidKey))
                .when(messageSource).getMessage(eq(invalidKey), any(), any(Locale.class));
            doReturn(fallbackMessage)
                .when(messageSource).getMessage(eq("default.error.message"), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(invalidKey);

            // Then
            assertThat(result).isEqualTo(fallbackMessage);
        }

        @Test
        @DisplayName("getMessage_withKeyAndMultipleArgs_returnsFormattedMessage")
        void getMessage_withKeyAndMultipleArgs_returnsFormattedMessage() {
            // Given
            String key = "type.mismatch";
            String expectedMessage = "Field 'id' should be of type Long";

            doReturn(expectedMessage)
                .when(messageSource).getMessage(eq(key), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(key, "id", "Long", "abc");

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("getMessage_withNullArgs_handlesGracefully")
        void getMessage_withNullArgs_handlesGracefully() {
            // Given
            String key = "global.error.message";
            String expectedMessage = "An error occurred";

            doReturn(expectedMessage)
                .when(messageSource).getMessage(eq(key), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(key, (Object[]) null);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }
    }

    // ========================================
    // GET MESSAGE BY ERROR CODE TESTS
    // ========================================

    @Nested
    @DisplayName("Get Message By ErrorCode Tests")
    class GetMessageByErrorCodeTests {

        @Test
        @DisplayName("getMessage_withErrorCode_returnsMessage")
        void getMessage_withErrorCode_returnsMessage() {
            // Given
            ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
            String expectedMessage = "Resource not found";

            doReturn(expectedMessage)
                .when(messageSource).getMessage(eq(errorCode.getKey()), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(errorCode);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("getMessage_withErrorCodeAndArgs_returnsFormattedMessage")
        void getMessage_withErrorCodeAndArgs_returnsFormattedMessage() {
            // Given
            ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
            String expectedMessage = "Database error";

            doReturn(expectedMessage)
                .when(messageSource).getMessage(eq(errorCode.getKey()), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(errorCode, "detail");

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("getMessage_withInvalidErrorCodeKey_returnsFallbackMessage")
        void getMessage_withInvalidErrorCodeKey_returnsFallbackMessage() {
            // Given
            ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
            String fallbackMessage = "An unexpected error occurred";

            doThrow(new NoSuchMessageException(errorCode.getKey()))
                .when(messageSource).getMessage(eq(errorCode.getKey()), any(), any(Locale.class));
            doReturn(fallbackMessage)
                .when(messageSource).getMessage(eq("default.error.message"), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(errorCode);

            // Then
            assertThat(result).isEqualTo(fallbackMessage);
        }

        @Test
        @DisplayName("getMessage_withErrorCodeAndMultipleArgs_formatsCorrectly")
        void getMessage_withErrorCodeAndMultipleArgs_formatsCorrectly() {
            // Given
            ErrorCode errorCode = ErrorCode.INVALID_DATA;
            String expectedMessage = "Invalid data";

            doReturn(expectedMessage)
                .when(messageSource).getMessage(eq(errorCode.getKey()), any(), any(Locale.class));

            // When
            String result = messageService.getMessage(errorCode, "arg1", "arg2", "arg3");

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }
    }
}
