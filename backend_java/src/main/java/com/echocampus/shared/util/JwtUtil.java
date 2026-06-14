package com.echocampus.shared.util;

import com.echocampus.shared.enums.RoleEnum;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, RoleEnum role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role.getValue())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public RoleEnum getRoleFromToken(String token) {
        String roleValue = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
        return RoleEnum.fromValue(roleValue);
    }

    public long getExpiration() {
        return expiration;
    }

    public TokenClaims parseToken(String token) {
        var claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String userId = claims.getSubject();
        String roleValue = claims.get("role", String.class);
        RoleEnum role = RoleEnum.fromValue(roleValue);
        return new TokenClaims(userId, role);
    }

    public record TokenClaims(String userId, RoleEnum role) {}
}
