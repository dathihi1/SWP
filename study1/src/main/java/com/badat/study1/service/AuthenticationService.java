package com.badat.study1.service;

import com.badat.study1.dto.JwtInfo;
import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.model.RedisToken;
import com.badat.study1.model.User;
import com.badat.study1.repository.RedisTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTokenRepository redisTokenRepository;

    public LoginResponse login(LoginRequest request){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        User user = (User) authenticate.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        //tra ve token
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(String token) throws ParseException {
        JwtInfo jwtInfo = jwtService.parseToken(token);
        String jwtId = jwtInfo.getJwtId();
        Date issueTime = jwtInfo.getIssueTime();
        Date expireTime = jwtInfo.getExpireTime();
        if(expireTime.before(new Date())) return;
        RedisToken redisToken = RedisToken.builder()
                .jwtID(jwtId)
                .expirationTime(expireTime.getTime() - issueTime.getTime())
                .build();
        redisTokenRepository.save(redisToken);
    }
}
