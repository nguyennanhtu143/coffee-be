package org.example.coffee.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.example.coffee.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TokenHelper {
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationTime;

    private static String STATIC_SECRET_KEY;
    private static long STATIC_EXPIRATION_TIME;

    @PostConstruct
    public void init() {
        STATIC_SECRET_KEY = this.secretKey;
        STATIC_EXPIRATION_TIME = this.expirationTime;
    }

    public static String generateToken(UserEntity userEntity) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + STATIC_EXPIRATION_TIME);

        return Jwts.builder()
                .claim("user_id",userEntity.getId())
                .claim("username",userEntity.getUsername())
                .setSubject(userEntity.getUsername())
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, STATIC_SECRET_KEY)
                .compact();
    }

    public static Long getUserIdFromToken(String accessToken) {
        accessToken = accessToken.substring(7);
        Claims claims = Jwts.parser()
                .setSigningKey(STATIC_SECRET_KEY)
                .parseClaimsJws(accessToken)
                .getBody();
        return claims.get("user_id", Long.class);
    }
}
