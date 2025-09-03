package org.itinov.bankApp.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itinov.bankApp.dto.AuthRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Controller to retrieve an access token from Keycloak using the Password grant.
 * Not a real authentication endpoint, just a proxy to Keycloak's token endpoint.
 * Mainly useful for Swagger UI testing.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/auth")
@Tag(name = "Authentication", description = "Endpoints to obtain tokens and authenticate")
public class AuthController {

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081/realms/bank-realm}")
    private String issuerUri;

    /**
     * Proxies a token request to Keycloak.
     * Example expected form parameters:
     * - username=jane
     * - password=pass123
     */
    @PostMapping(
        value = "/token",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Obtain an access token from Keycloak",
        description = "Accepts JSON body with username and password, then proxies a Resource Owner Password Credentials " +
            "(password) grant to Keycloak for the client 'bank-app'.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Keycloak error",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<String> getToken(@Valid @RequestBody AuthRequest request) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "bank-app");
        form.add("username", request.username());
        form.add("password", request.password());

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, httpEntity, String.class);
            // Pass through status and body from Keycloak
            return ResponseEntity
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            // Propagate Keycloak error body and status for easier troubleshooting
            return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getResponseBodyAsString());
        }
    }
}
