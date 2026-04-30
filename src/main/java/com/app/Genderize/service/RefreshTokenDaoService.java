package com.app.Genderize.service;

import com.app.Genderize.model.RefreshToken;
import com.app.Genderize.model.User;
import com.app.Genderize.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenDaoService {

    private final RefreshTokenRepository repository;

    public RefreshToken create(User user, String rawToken) {
        log.info("Generating refresh token");
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(300));
        return repository.save(token);
    }

    public RefreshToken validate(String rawToken) {
        String hash = hash(rawToken);

        RefreshToken token = repository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Expired or revoked");
        }

        return token;
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }

    private String hash(String token) {
        return DigestUtils.sha256Hex(token);
    }
}
