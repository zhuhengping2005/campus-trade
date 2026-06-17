package com.campus.trade.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    
    // 固定密钥和过期时间
    private static final String SECRET = "CampusTradeSecretKey2024";
    private static final long EXPIRATION = 86400000L; // 24小时

    public static String generateToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    public static Long getUserIdFromToken(String token) {
        Map<String, Object> claims = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
        return (Long) claims.get("userId");
    }
}
