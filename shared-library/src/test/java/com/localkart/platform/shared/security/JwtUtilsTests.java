package com.localkart.platform.shared.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTests {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Set a secure 256-bit key for HMAC-SHA256 signature
        ReflectionTestUtils.setField(jwtUtils, "secret", "dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLWxvY2Fsa2FydC1wbGF0Zm9ybS1lbnRlcnByaXNlLWNvbW1lcmNlLXBsYXRmb3Jt");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 86400000L); // 24 hours
    }

    @Test
    void testTokenGenerationAndParsing() {
        String username = "john.doe@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", "ROLE_USER,ROLE_ADMIN");
        claims.put("userId", "12345");

        String token = jwtUtils.generateToken(username, claims);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        String extractedUsername = jwtUtils.extractUsername(token);
        assertEquals(username, extractedUsername);

        String roles = jwtUtils.extractClaim(token, c -> c.get("roles", String.class));
        assertEquals("ROLE_USER,ROLE_ADMIN", roles);

        String userId = jwtUtils.extractClaim(token, c -> c.get("userId", String.class));
        assertEquals("12345", userId);

        assertTrue(jwtUtils.validateToken(token, username));
        assertFalse(jwtUtils.isTokenExpired(token));
    }
}
