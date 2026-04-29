package com.example.payment_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac";

    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        jwtService.init();
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }


    private String buildToken(String subject, String role, Date expiry) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    private String validToken(String subject, String role) {
        return buildToken(subject, role, new Date(System.currentTimeMillis() + 60_000));
    }


    @Test
    void extractPrincipal_returnsUserPrincipal_forValidToken() {
        String token = validToken("user-123", "ROLE_USER");

        Optional<UserPrincipal> result = jwtService.extractPrincipal("Bearer " + token);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user-123");
        assertThat(result.get().getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void extractPrincipal_returnsEmpty_whenHeaderIsNull() {
        assertThat(jwtService.extractPrincipal(null)).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_whenHeaderLacksBearer() {
        String token = validToken("user-123", "ROLE_USER");
        assertThat(jwtService.extractPrincipal(token)).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_whenHeaderIsOnlyBearerPrefix() {
        assertThat(jwtService.extractPrincipal("Bearer ")).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_forMalformedToken() {
        assertThat(jwtService.extractPrincipal("Bearer this.is.garbage")).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_forTokenSignedWithWrongKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "sajkhdbasdhj-asjda-asjkdna-abcdefghij".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .claim("role", "ROLE_USER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.extractPrincipal("Bearer " + token)).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_forExpiredToken() {
        String token = buildToken("user-123", "ROLE_USER", new Date(System.currentTimeMillis() - 1_000));

        assertThat(jwtService.extractPrincipal("Bearer " + token)).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_whenSubjectClaimIsMissing() {
        String token = Jwts.builder()
                .claim("role", "ROLE_USER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        assertThat(jwtService.extractPrincipal("Bearer " + token)).isEmpty();
    }

    @Test
    void extractPrincipal_returnsEmpty_whenRoleClaimIsMissing() {
        String token = Jwts.builder()
                .subject("user-123")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        assertThat(jwtService.extractPrincipal("Bearer " + token)).isEmpty();
    }

    @Test
    void extractPrincipal_handlesAdminRole() {
        String token = validToken("admin-42", "ROLE_ADMIN");

        Optional<UserPrincipal> result = jwtService.extractPrincipal("Bearer " + token);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("admin-42");
        assertThat(result.get().getRole()).isEqualTo("ROLE_ADMIN");
    }
}