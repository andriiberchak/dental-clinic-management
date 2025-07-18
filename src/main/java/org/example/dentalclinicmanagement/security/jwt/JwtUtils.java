package org.example.dentalclinicmanagement.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;


    @Value("${jwt.issuer}")
    private String jwtIssuer;

    private SecretKey key;

    @PostConstruct
    private void initializeKey() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwtToken(UserDetailsImpl userPrincipal) {
        return generateTokenFromEmail(userPrincipal.getEmail());
    }

    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        return Jwts.builder()
                .setIssuer(jwtIssuer)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public String getEmailFromJwtToken(String token) {
        return getJwtParser().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            getJwtParser().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private JwtParser getJwtParser() {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(jwtIssuer)
                .build();
    }
}