package org.demo.carbe.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
@Component
public class JwtHelper {

    private final JwtProperties jwtProperties;

    public SecretKey getHmacKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretKey()));
    }

    public String createToken(Map<String, Object> claims, String subject) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtProperties.getValidityInMs());
        return Jwts.builder()
                .claims(claims)
                .issuer(jwtProperties.getIssuer())
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(expiryDate)
                .signWith(getHmacKey())
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaimBody(token, Claims::getSubject);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaimBody(token, Claims::getExpiration);
    }

    private <T> T extractClaimHeader(String token, Function<JwsHeader, T> claimsResolver) {
        final Jws<Claims> jwsClaims = extractAllClaims(token);
        return claimsResolver.apply(jwsClaims.getHeader());
    }

    private <T> T extractClaimBody(String token, Function<Claims, T> claimsResolver) {
        final Jws<Claims> jwsClaims = extractAllClaims(token);
        return claimsResolver.apply(jwsClaims.getPayload());
    }

    private Jws<Claims> extractAllClaims(String token) {
        // parse a bearer token and return Jws<Claims> object
        return Jwts.parser()
                .verifyWith(getHmacKey())
                .build()
                .parseSignedClaims(token);
    }
}
