package com.freelancehub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtil - utility for JWT (JSON Web Token) operations.
 *
 * What is a JWT?
 *   A JWT is a compact, URL-safe token with 3 parts separated by dots:
 *   header.payload.signature
 *
 *   - Header: algorithm used (HS256)
 *   - Payload: claims (user email, expiration date, etc.)
 *   - Signature: proves the token wasn't tampered with
 *
 * How it works in this app:
 *   1. User logs in -> we create a JWT with their email inside
 *   2. We send the JWT to the client (browser)
 *   3. Browser stores it in localStorage
 *   4. Every subsequent request includes: "Authorization: Bearer <token>"
 *   5. We validate the token on each request - no need for a session DB
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expirationMs;

    /**
     * Creates a signing key from the secret string.
     * HMAC-SHA256 requires at least 256 bits (32 bytes).
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate a JWT for the given user.
     *
     * @param userDetails - Spring Security user (contains username/email)
     * @return signed JWT string
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return buildToken(claims, userDetails.getUsername());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)                              // the user's email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract the email (subject) from a token.
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validate the token:
     *   - Is the signature valid? (wasn't tampered with)
     *   - Is the email in the token the same as the logged-in user?
     *   - Has the token expired?
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}
