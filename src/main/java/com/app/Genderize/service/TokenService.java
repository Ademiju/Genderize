package com.app.Genderize.service;

import com.app.Genderize.config.auth.JwtService;
import com.app.Genderize.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtService jwtService;
    private final RefreshTokenDaoService refreshTokenDaoService;

    public AuthResponse issue(User user) {

        String access = jwtService.generateAccessToken(user);
        String refreshRaw = UUID.randomUUID().toString();

        refreshTokenDaoService.create(user, refreshRaw);

        return new AuthResponse("success", access, refreshRaw);
    }

    public record AuthResponse(
            String status,
            String access_token,
            String refresh_token
    ) {}
}
