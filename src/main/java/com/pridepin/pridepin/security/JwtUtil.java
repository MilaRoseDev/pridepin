package com.pridepin.pridepin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT creation and validation. Uses HMAC-SHA with a base64-encoded secret from configuration.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Builds a signed JWT with subject = username, issued-at and expiration from config.
     *
     * @param userDetails used to read the username (subject)
     * @return compact JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the subject (username) from the JWT payload. Does not validate expiration.
     *
     * @param token the JWT string
     * @return username from the token subject
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Checks that the token is signed correctly, not expired, and the subject matches the given user.
     *
     * @param token       the JWT string
     * @param userDetails the current user to match against
     * @return true if valid, false on parse/expiry/mismatch (logs a warning)
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /** Returns true if the token's expiration timestamp is in the past. */
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /** Parses and verifies the JWT signature, returns the payload (claims). */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Decodes the base64 secret and builds an HMAC-SHA key for signing/verification. */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
