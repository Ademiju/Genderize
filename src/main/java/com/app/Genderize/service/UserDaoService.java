package com.app.Genderize.service;

import com.app.Genderize.enums.Role;
import com.app.Genderize.model.User;
import com.app.Genderize.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDaoService {
    private final UserRepository userRepository;


    public User findById(UUID uuid) {
        return userRepository.findById(uuid).orElse(null);
    }

    public User findOrCreateGithubUser(Map<String, Object> githubUser) {
        String githubId = String.valueOf(githubUser.get("id"));

        return userRepository.findByGithubId(githubId)
                .map(user -> {
                    user.setLastLoginAt(Instant.now());
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(UUID.randomUUID());
                    user.setGithubId(githubId);
                    user.setUsername((String) githubUser.get("login"));
                    user.setEmail((String) githubUser.get("email"));
//                    user.setAvatarUrl((String) githubUser.get("avatar_url"));
                    user.setRole(Role.analyst);
                    user.setCreatedAt(Instant.now());
                    return userRepository.save(user);
                });
    }
}
