package com.badat.study1.service;

import com.badat.study1.dto.JwtInfo;
import com.badat.study1.model.User;
import com.badat.study1.repository.RedisTokenRepository;
import org.springframework.security.core.userdetails.UserDetails;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secret-key}")
    private String secret;
    private final RedisTokenRepository redisTokenRepository;

    public String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        Date issueTime = new Date();
        Date expirationTime = Date.from(issueTime.toInstant().plusSeconds(60 * 60)); // 1 hour
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jweObject = new JWSObject(header, payload);
        try {
            jweObject.sign(new MACSigner(secret));
            return jweObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

    }

    public String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        Date issueTime = new Date();
        Date expirationTime = Date.from(issueTime.toInstant().plusSeconds(24 * 60 * 60));
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jweObject = new JWSObject(header, payload);
        try {
            jweObject.sign(new MACSigner(secret));
            return jweObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean verifyToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        
        // Check if token is expired first (more efficient)
        if (expirationTime.before(new Date())) {
            return false; // Token has expired
        }
        
        // Check if token is blacklisted (logged out) - with fallback if Redis is unavailable
        try {
            if (redisTokenRepository.existsById(jwtId)) {
                return false; // Token has been blacklisted
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping blacklist check: {}", e.getMessage());
            // Continue without blacklist check if Redis is unavailable
        }
        
        // Verify token signature
        boolean signatureValid = signedJWT.verify(new MACVerifier(secret));
        if (!signatureValid) {
            return false;
        }
        return signatureValid;
    }

    public String extractUsername(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            boolean usernameMatches = username.equals(userDetails.getUsername());
            boolean tokenValid = verifyToken(token);
            
            // Log detailed token info for debugging
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            Date now = new Date();
            
            if (!usernameMatches) {
                log.warn("Token validation failed: username mismatch. Token username: {}, Expected: {}", username, userDetails.getUsername());
            }
            if (!tokenValid) {
                log.warn("Token validation failed: token invalid or expired for user: {} (expires: {}, now: {})", 
                        username, expirationTime, now);
            }
            
            return usernameMatches && tokenValid;
        } catch (Exception e) {
            log.error("Token validation failed with exception for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            return false;
        }
    }

    public JwtInfo parseToken(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date issueTime = signedJWT.getJWTClaimsSet().getIssueTime();
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        return JwtInfo.builder()
                .jwtId(jwtId)
                .issueTime(issueTime)
                .expireTime(expirationTime)
                .build();
    }
    
    public boolean isTokenExpiringSoon(String token, int minutesThreshold) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            Date now = new Date();
            long timeUntilExpiry = expirationTime.getTime() - now.getTime();
            long thresholdMs = minutesThreshold * 60 * 1000; // Convert minutes to milliseconds
            
            return timeUntilExpiry <= thresholdMs && timeUntilExpiry > 0;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true; // Assume expired if we can't parse
        }
    }
}
