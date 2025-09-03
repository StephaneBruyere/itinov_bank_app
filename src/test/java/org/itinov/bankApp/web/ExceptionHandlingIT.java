package org.itinov.bankApp.web;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @RestController
    static class ThrowingController {
        @PostMapping("/api/accounts/transfer")
        public void transfer() {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
    }
}
