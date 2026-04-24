package com.example.payment_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<UserPrincipal> extractPrincipal(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = bearerToken.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role   = claims.get("role", String.class);

            if (userId == null || role == null) {
                log.warn("JWT missing 'sub' or 'role' claim");
                return Optional.empty();
            }
            return Optional.of(new UserPrincipal(userId, role));

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}