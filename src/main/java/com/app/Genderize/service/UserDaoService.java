package com.app.Genderize.service;

import com.app.Genderize.enums.Role;
import com.app.Genderize.model.User;
import com.app.Genderize.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDaoService {
    private final UserRepository userRepository;


    public User findById(UUID uuid) {
        return userRepository.findById(uuid).orElse(null);
    }

    public User findOrCreateGithubUser(Map<String, Object> githubUser) {
        log.info("Start findOrCreateGithubUser service");
        String githubId = String.valueOf(githubUser.get("id"));
        String username = (String) githubUser.get("login");
        String email = (String) githubUser.get("email");
        String avatarUrl = (String) githubUser.get("avatar_url");

        return userRepository.findByGithubId(githubId)
                .map(user -> {
                    user.setUsername(username);
                    user.setEmail(email);
                    user.setAvatarUrl(avatarUrl);
                    user.setLastLoginAt(Instant.now());
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(UuidCreator.getTimeOrderedEpoch());
                    user.setGithubId(githubId);
                    user.setUsername(username);
                    user.setEmail(email);
                    user.setAvatarUrl(avatarUrl);
                    user.setRole(Role.analyst);
                    user.setActive(true);
                    user.setLastLoginAt(Instant.now());
                    user.setCreatedAt(Instant.now());
                    return userRepository.save(user);
                });
    }

    public User findOrCreateTestUser(String code) {
        log.info("Start findOrCreateTestUser service");

        Optional<User> optionalUser = userRepository.findByGithubId(code.concat("001"));
        if (optionalUser.isEmpty()){
             User user =
                     User.builder()
                            .githubId(code.equals("test_code") ? "test_code001" : "analyst_test_code001")
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .role(code.equals("test_code") ? Role.admin : Role.analyst)
                            .email(code.equals("test_code") ? "admintester@email.com" : "analysttester@email.com")
                            .avatarUrl("https://avatars.githubusercontent.com/u/githubid?v=4")
                            .isActive(true)
                            .lastLoginAt(Instant.now())
                            .createdAt(Instant.now())
                            .username(code.equals("test_code") ? "Admin001": "Analyst001")
                            .build();
            return userRepository.save(user);
        }
        return optionalUser.get();
    }
}
