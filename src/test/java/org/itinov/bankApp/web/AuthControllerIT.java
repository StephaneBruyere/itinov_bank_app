package org.itinov.bankApp.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://mocked-issuer/realms/bank-realm"
})
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AuthController authController;

    @Test
    @DisplayName("AuthController returns 400 for validation errors (missing fields)")
    void tokenValidationError() throws Exception {
        mockMvc.perform(post("/api/public/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AuthController proxies token request successfully")
    void tokenOk() throws Exception {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);
        DefaultResponseCreator response = MockRestResponseCreators
            .withSuccess("{\"access_token\":\"abc\"}", MediaType.APPLICATION_JSON);
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers
                    .requestTo("http://mocked-issuer/realms/bank-realm/protocol/openid-connect/token"))
            .andExpect(MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
            .andRespond(response);

        // Inject our RestTemplate into the controller via reflection
        ReflectionTestUtils.setField(authController, "restTemplate", rt);

        mockMvc.perform(post("/api/public/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"john\",\"password\":\"doe\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("access_token")));

        server.verify();
    }

    @Test
    @DisplayName("AuthController propagates Keycloak error response")
    void tokenErrorPropagated() throws Exception {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers
                    .requestTo("http://mocked-issuer/realms/bank-realm/protocol/openid-connect/token"))
            .andExpect(MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
            .andRespond(MockRestResponseCreators.withUnauthorizedRequest()
                .body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));

        ReflectionTestUtils.setField(authController, "restTemplate", rt);

        mockMvc.perform(post("/api/public/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bad\",\"password\":\"creds\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid_grant")));

        server.verify();
    }
}
