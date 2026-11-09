package com.skillmate.security;

import com.skillmate.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${skillmate.jwt.secret}") String secret,
                      @Value("${skillmate.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(expirationMinutes);
    }

    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail()).claim("role", user.getRole().name())
                .issuedAt(now).expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key).compact();
    }

    /** Throws JwtException if the token is invalid or expired. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Duration getTtl() { return ttl; }
}
