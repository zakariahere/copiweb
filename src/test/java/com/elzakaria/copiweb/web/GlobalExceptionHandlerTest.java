package com.elzakaria.copiweb.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void detectsExpectedDisconnectFromServletContainerMessage() {
        var ex = new IllegalStateException("Servlet container error notification for disconnected client");

        assertThat(GlobalExceptionHandler.isExpectedClientDisconnect(ex)).isTrue();
    }

    @Test
    void doesNotTreatGenericCommittedExceptionAsDisconnect() {
        var ex = new IllegalStateException("Something else went wrong");

        assertThat(GlobalExceptionHandler.isExpectedClientDisconnect(ex)).isFalse();
    }

    @Test
    void returnsInternalServerErrorForNormalUnhandledExceptions() {
        var response = new MockHttpServletResponse();

        var result = handler.handleGeneral(new IllegalArgumentException("boom"), response);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode().value()).isEqualTo(500);
        assertThat(result.getBody()).containsEntry("error", "boom");
    }
}
