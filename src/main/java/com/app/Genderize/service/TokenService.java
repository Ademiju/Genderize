package com.app.Genderize.service;

import com.app.Genderize.config.auth.JwtService;
import com.app.Genderize.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtService jwtService;
    private final RefreshTokenDaoService refreshTokenDaoService;

    public AuthResponse issue(User user) {
        log.info("Start token issue service");

        String access = jwtService.generateAccessToken(user);
        String refreshRaw = UUID.randomUUID().toString();

        refreshTokenDaoService.create(user, refreshRaw);
        log.info("Token successfully issued");
        return new AuthResponse("success", access, refreshRaw, user.getUsername(), user.getRole().name());
    }

    public record AuthResponse(
            String status,
            String access_token,
            String refresh_token,
            String username,
            String role
    ) {}
}
