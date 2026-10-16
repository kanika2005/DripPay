package com.drippay.security;

import com.drippay.config.DrippayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final DrippayProperties properties;

    public JwtService(DrippayProperties properties) {
        this.properties = properties;
    }

    public String generateToken(UserPrincipal principal) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("role", principal.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.getJwt().getExpirationMs()))
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parse(token).get("uid", Long.class);
    }

    public boolean isValid(String token, UserPrincipal principal) {
        String email = extractEmail(token);
        return email.equals(principal.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return parse(token).getExpiration().before(new Date());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        String secret = properties.getJwt().getSecret();
        byte[] keyBytes = secret.length() >= 32
                ? secret.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
