package com.vinicius.vanguarda.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinicius.vanguarda.infrastructure.security.JwtService;
import com.vinicius.vanguarda.infrastructure.security.SecurityConfig;
import com.vinicius.vanguarda.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "vcb.security.auth-client-id=test-client",
        "vcb.security.auth-client-secret=test-secret",
        "vcb.security.jwt-secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktMzJieXRlcyEh",
        "vcb.security.jwt-expiration-hours=24"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean JwtService jwtService;

    @Test
    void shouldReturn200WithTokenOnValidCredentials() throws Exception {
        when(jwtService.generateToken("test-client")).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationMillis()).thenReturn(86400000L);

        String body = objectMapper.writeValueAsString(Map.of(
                "clientId", "test-client",
                "clientSecret", "test-secret"
        ));

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void shouldReturn401OnWrongClientId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "clientId", "wrong-client",
                "clientSecret", "test-secret"
        ));

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void shouldReturn401OnWrongClientSecret() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "clientId", "test-client",
                "clientSecret", "wrong-secret"
        ));

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void shouldReturn400WhenClientIdMissing() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "clientSecret", "test-secret"
        ));

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturn400WhenClientSecretMissing() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "clientId", "test-client"
        ));

        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturn400OnEmptyBody() throws Exception {
        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void tokenEndpointShouldBePublicNoAuthRequired() throws Exception {
        // Verify /v1/auth/token is accessible without any Authorization header
        when(jwtService.generateToken("test-client")).thenReturn("tok");
        when(jwtService.getExpirationMillis()).thenReturn(86400000L);

        String body = objectMapper.writeValueAsString(Map.of(
                "clientId", "test-client",
                "clientSecret", "test-secret"
        ));

        // No Authorization header — must not return 401
        mockMvc.perform(post("/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
