package com.elara.app.inventory_service.exceptions;

import com.elara.app.inventory_service.utils.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Custom Exception Classes")
class CustomExceptionsTest {

    // ========================================
    // BASE EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("BaseException")
    class BaseExceptionTests {

        @Test
        @DisplayName("constructor_withErrorCodeAndMessage_setsFieldsCorrectly")
        void constructor_withErrorCodeAndMessage_setsFieldsCorrectly() {
            // Given
            ErrorCode errorCode = ErrorCode.INVALID_DATA;
            String message = "Invalid input data provided";

            // When
            BaseException exception = new BaseException(errorCode, message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_DATA.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.INVALID_DATA.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withDifferentErrorCode_setsCorrectCode")
        void constructor_withDifferentErrorCode_setsCorrectCode() {
            // Given
            ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
            String message = "Database operation failed";

            // When
            BaseException exception = new BaseException(errorCode, message);

            // Then
            assertThat(exception.getCode()).isEqualTo(1001);
            assertThat(exception.getValue()).isEqualTo("DATABASE_ERROR");
        }
    }

    // ========================================
    // RESOURCE NOT FOUND EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("constructor_setsResourceNotFoundErrorCode")
        void constructor_setsResourceNotFoundErrorCode() {
            // Given
            String message = "InventoryItem with id '123' not found";

            // When
            ResourceNotFoundException exception = new ResourceNotFoundException(message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withResourceNotFoundMessage_setsProvidedMessage")
        void constructor_withResourceNotFoundMessage_setsProvidedMessage() {
            // Given
            String message = "UOM with id '999' not found";

            // When
            ResourceNotFoundException exception = new ResourceNotFoundException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(1004);
        }
    }

    // ========================================
    // RESOURCE CONFLICT EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("ResourceConflictException")
    class ResourceConflictExceptionTests {

        @Test
        @DisplayName("constructor_setsResourceConflictErrorCode")
        void constructor_setsResourceConflictErrorCode() {
            // Given
            String message = "InventoryItem with name 'Steel Bolt M10' already exists";

            // When
            ResourceConflictException exception = new ResourceConflictException(message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.RESOURCE_CONFLICT.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withConflictMessage_setsProvidedMessage")
        void constructor_withConflictMessage_setsProvidedMessage() {
            // Given
            String message = "Duplicate entry detected";

            // When
            ResourceConflictException exception = new ResourceConflictException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(1003);
        }
    }

    // ========================================
    // INVALID DATA EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("InvalidDataException")
    class InvalidDataExceptionTests {

        @Test
        @DisplayName("constructor_setsInvalidDataErrorCode")
        void constructor_setsInvalidDataErrorCode() {
            // Given
            String message = "Price cannot be negative";

            // When
            InvalidDataException exception = new InvalidDataException(message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_DATA.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.INVALID_DATA.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withValidationError_setsProvidedMessage")
        void constructor_withValidationError_setsProvidedMessage() {
            // Given
            String message = "Name must not be blank";

            // When
            InvalidDataException exception = new InvalidDataException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(1002);
        }
    }

    // ========================================
    // DATABASE EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("DatabaseException")
    class DatabaseExceptionTests {

        @Test
        @DisplayName("constructor_setsDatabaseErrorCode")
        void constructor_setsDatabaseErrorCode() {
            // Given
            String message = "Failed to save InventoryItem";

            // When
            DatabaseException exception = new DatabaseException(message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.DATABASE_ERROR.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.DATABASE_ERROR.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withDatabaseErrorMessage_setsProvidedMessage")
        void constructor_withDatabaseErrorMessage_setsProvidedMessage() {
            // Given
            String message = "Connection timeout";

            // When
            DatabaseException exception = new DatabaseException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(1001);
        }
    }

    // ========================================
    // SERVICE UNAVAILABLE EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("ServiceUnavailableException")
    class ServiceUnavailableExceptionTests {

        @Test
        @DisplayName("constructor_setsServiceUnavailableErrorCode")
        void constructor_setsServiceUnavailableErrorCode() {
            // Given
            String message = "UOM Service is temporarily unavailable";

            // When
            ServiceUnavailableException exception = new ServiceUnavailableException(message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withServiceDownMessage_setsProvidedMessage")
        void constructor_withServiceDownMessage_setsProvidedMessage() {
            // Given
            String message = "External service returned 503";

            // When
            ServiceUnavailableException exception = new ServiceUnavailableException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(1005);
        }
    }

    // ========================================
    // UNEXPECTED ERROR EXCEPTION TESTS
    // ========================================

    @Nested
    @DisplayName("UnexpectedErrorException")
    class UnexpectedErrorExceptionTests {

        @Test
        @DisplayName("constructor_setsUnexpectedErrorCode")
        void constructor_setsUnexpectedErrorCode() {
            // Given
            String message = "An unexpected error occurred during processing";

            // When
            UnexpectedErrorException exception = new UnexpectedErrorException(message);

            // Then
            assertThat(exception.getCode()).isEqualTo(ErrorCode.UNEXPECTED_ERROR.getCode());
            assertThat(exception.getValue()).isEqualTo(ErrorCode.UNEXPECTED_ERROR.getValue());
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("constructor_withUnexpectedErrorMessage_setsProvidedMessage")
        void constructor_withUnexpectedErrorMessage_setsProvidedMessage() {
            // Given
            String message = "Null pointer encountered";

            // When
            UnexpectedErrorException exception = new UnexpectedErrorException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(1006);
        }
    }
}
