package com.badat.study1.configuration;

import com.badat.study1.service.JwtService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtDecoderConfiguration implements JwtDecoder {
    @Value("${jwt.secret-key}")
    private String secret;
    private final JwtService jwtService;
    private NimbusJwtDecoder jwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        log.info( token);
        try {
            if (!jwtService.verifyToken(token)) throw new RuntimeException("Invalid token");
            if (Objects.isNull(jwtDecoder)) {
                SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HS512");
                jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();

            }
        } catch (ParseException | JOSEException e) {
            throw new RuntimeException(e);
        }
        return jwtDecoder.decode(token);
    }
}
