package com.resqcloud.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid JWT and extract correct user ID and claims")
    void testTokenGenerationAndValidation() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId,
                "testuser",
                "test@resqcloud.com",
                "encodedPassword",
                "Test User",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = tokenProvider.generateToken(auth);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 segments");

        boolean isValid = tokenProvider.validateToken(token);
        assertTrue(isValid, "Token should be valid");

        String extractedId = tokenProvider.getUserIdFromToken(token);
        assertEquals(userId.toString(), extractedId);
    }

    @Test
    @DisplayName("Should reject malformed or tampered JWT")
    void testMalformedTokenRejected() {
        String invalidToken = "ey328912.invalid.signature";
        assertFalse(tokenProvider.validateToken(invalidToken));
    }
}
