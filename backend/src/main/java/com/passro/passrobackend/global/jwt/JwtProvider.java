package com.passro.passrobackend.global.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigninKey(){
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long accountId, String role){
        return createToken(accountId, role, jwtProperties.getAccessTokenExpiration());
    }

    public String createRefreshToken(Long accountId){
        return createToken(accountId, null, jwtProperties.getRefreshTokenExpiration());
    }

    private String createToken(Long accountId, String role, long expiration){
        Date now = new Date();
        Date expiry = new Date(now.getTime()  + expiration);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(accountId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigninKey());

        if(role != null)
            builder.claim("role", role);

        return builder.compact();
    }

    public Long getAccountId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
