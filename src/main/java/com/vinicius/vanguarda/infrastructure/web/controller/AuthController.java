package com.vinicius.vanguarda.infrastructure.web.controller;

import com.vinicius.vanguarda.infrastructure.security.JwtService;
import com.vinicius.vanguarda.infrastructure.web.dto.request.AuthRequest;
import com.vinicius.vanguarda.infrastructure.web.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "Authentication — obtain a Bearer token")
public class AuthController {

    private final JwtService jwtService;
    private final String clientId;
    private final String clientSecret;

    public AuthController(JwtService jwtService,
                          @Value("${vcb.security.auth-client-id}") String clientId,
                          @Value("${vcb.security.auth-client-secret}") String clientSecret) {
        this.jwtService = jwtService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @PostMapping("/token")
    @Operation(summary = "Obtain a JWT — send clientId + clientSecret, receive Bearer token")
    public ResponseEntity<AuthResponse> token(@Valid @RequestBody AuthRequest request) {
        if (!clientId.equals(request.clientId()) || !clientSecret.equals(request.clientSecret())) {
            return ResponseEntity.status(401).build();
        }
        String token = jwtService.generateToken(request.clientId());
        long expiresIn = jwtService.getExpirationMillis() / 1000;
        return ResponseEntity.ok(new AuthResponse(token, expiresIn));
    }
}
