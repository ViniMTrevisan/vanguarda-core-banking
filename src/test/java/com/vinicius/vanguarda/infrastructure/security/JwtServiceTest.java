package com.vinicius.vanguarda.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    // Base64("test-secret-key-for-unit-tests-only-32bytes!!")
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktMzJieXRlcyEh";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 24);
    }

    @Test
    void shouldGenerateNonNullToken() {
        String token = jwtService.generateToken("vcb-admin");
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldExtractCorrectSubject() {
        String token = jwtService.generateToken("vcb-admin");
        assertThat(jwtService.extractSubject(token)).isEqualTo("vcb-admin");
    }

    @Test
    void shouldValidateValidToken() {
        String token = jwtService.generateToken("vcb-admin");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken("vcb-admin");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, 0); // 0h = expires immediately
        String token = shortLived.generateToken("vcb-admin");
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void shouldRejectGarbageToken() {
        assertThat(jwtService.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void shouldRejectEmptyToken() {
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void shouldReturnCorrectExpirationMillis() {
        JwtService service = new JwtService(SECRET, 2);
        assertThat(service.getExpirationMillis()).isEqualTo(2 * 3600 * 1000L);
    }

    @Test
    void differentSubjectsShouldProduceDifferentTokens() {
        String token1 = jwtService.generateToken("user-a");
        String token2 = jwtService.generateToken("user-b");
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractSubject(token1)).isEqualTo("user-a");
        assertThat(jwtService.extractSubject(token2)).isEqualTo("user-b");
    }
}
