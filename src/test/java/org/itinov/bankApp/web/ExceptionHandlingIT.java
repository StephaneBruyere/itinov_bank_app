package org.itinov.bankApp.web;

import jakarta.persistence.EntityNotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExceptionHandlingIT {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
    }

    @Test
    @DisplayName("Global ExceptionControllerAdvice returns 400 with message for IllegalArgumentException")
    void exceptionAdviceReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer fake-token") // Spring passera dans JwtDecoder
                .param("fromAccountId", "1")
                .param("toAccountId", "1")
                .param("amount", "10")
                .param("performedBy", "it-test")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(Matchers.containsString("Cannot transfer to the same account")));
    }

    @Test
    @DisplayName("Global ExceptionControllerAdvice returns 400 with message for RuntimeException")
    void exceptionAdviceReturnsBadRequestForRuntime() throws Exception {
        mockMvc.perform(get("/api/test/runtime")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(Matchers.containsString("Something bad happened")));
    }

    @Test
    @DisplayName("Global ExceptionControllerAdvice returns 404 with message for EntityNotFoundException")
    void exceptionAdviceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/test/not-found")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content().string(Matchers.containsString("Entity with id 42 not found")));
    }

    @Test
    @DisplayName("Global ExceptionControllerAdvice returns 403 with empty body for AccessDeniedException")
    void exceptionAdviceReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/test/forbidden")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(content().string(""));
    }

    @RestController
    static class ThrowingController {
        @PostMapping("/api/accounts/transfer")
        public void transfer() {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        @GetMapping("/api/test/runtime")
        public void runtime() {
            throw new RuntimeException("Something bad happened");
        }

        @GetMapping("/api/test/not-found")
        public void notFound() {
            throw new EntityNotFoundException("Entity with id 42 not found");
        }

        @GetMapping("/api/test/forbidden")
        public void forbidden() {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
