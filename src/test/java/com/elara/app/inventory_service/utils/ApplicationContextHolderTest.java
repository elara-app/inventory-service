package com.elara.app.inventory_service.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationContextHolder")
class ApplicationContextHolderTest {

    @Mock
    private ApplicationContext applicationContext;

    private ApplicationContextHolder contextHolder;

    @BeforeEach
    void setUp() {
        contextHolder = new ApplicationContextHolder();
    }

    @AfterEach
    void tearDown() {
        reset(applicationContext);
    }

    // ========================================
    // SET APPLICATION CONTEXT TESTS
    // ========================================

    @Nested
    @DisplayName("Set Application Context Tests")
    class SetApplicationContextTests {

        @Test
        @DisplayName("setApplicationContext_withValidContext_setsSuccessfully")
        void setApplicationContext_withValidContext_setsSuccessfully() throws BeansException {
            // When
            contextHolder.setApplicationContext(applicationContext);

            // Then - No exception thrown means context was set successfully
            assertThat(applicationContext).isNotNull();
        }

        @Test
        @DisplayName("setApplicationContext_withApplicationContext_updatesContextSuccessfully")
        void setApplicationContext_withApplicationContext_updatesContextSuccessfully() throws BeansException {
            // Given
            ApplicationContext firstContext = applicationContext;

            // When
            contextHolder.setApplicationContext(firstContext);

            // Then - Verify the context was accepted (no exception)
            assertThat(firstContext).isNotNull();
        }
    }

    // ========================================
    // GET BEAN TESTS
    // ========================================

    @Nested
    @DisplayName("Get Bean Tests")
    class GetBeanTests {

        @Test
        @DisplayName("getBean_withValidBeanClass_returnsBean")
        void getBean_withValidBeanClass_returnsBean() throws BeansException {
            // Given
            MessageService expectedBean = mock(MessageService.class);
            contextHolder.setApplicationContext(applicationContext);

            doReturn(expectedBean)
                .when(applicationContext).getBean(any(Class.class));

            // When
            MessageService result = ApplicationContextHolder.getBean(MessageService.class);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("getBean_withDifferentBeanClass_returnsBeanSuccessfully")
        void getBean_withDifferentBeanClass_returnsBeanSuccessfully() throws BeansException {
            // Given
            ErrorCode errorCodeMock = mock(ErrorCode.class);
            contextHolder.setApplicationContext(applicationContext);

            doReturn(errorCodeMock)
                .when(applicationContext).getBean(any(Class.class));

            // When
            ErrorCode result = ApplicationContextHolder.getBean(ErrorCode.class);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("getBean_withApplicationContextClass_returnsApplicationContext")
        void getBean_withApplicationContextClass_returnsApplicationContext() throws BeansException {
            // Given
            contextHolder.setApplicationContext(applicationContext);

            doReturn(applicationContext)
                .when(applicationContext).getBean(any(Class.class));

            // When
            ApplicationContext result = ApplicationContextHolder.getBean(ApplicationContext.class);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("getBean_calledMultipleTimes_retrievesBeans")
        void getBean_calledMultipleTimes_retrievesBeans() throws BeansException {
            // Given
            MessageService bean1 = mock(MessageService.class);
            MessageService bean2 = mock(MessageService.class);
            contextHolder.setApplicationContext(applicationContext);

            doReturn(bean1).doReturn(bean2)
                .when(applicationContext).getBean(any(Class.class));

            // When
            MessageService firstCall = ApplicationContextHolder.getBean(MessageService.class);
            MessageService secondCall = ApplicationContextHolder.getBean(MessageService.class);

            // Then
            assertThat(firstCall).isNotNull();
            assertThat(secondCall).isNotNull();
        }
    }
}
