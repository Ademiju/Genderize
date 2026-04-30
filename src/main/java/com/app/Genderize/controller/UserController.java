package com.app.Genderize.controller;

import com.app.Genderize.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Authentication required"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail() == null ? "" : user.getEmail(),
                        "avatar_url", user.getAvatarUrl() == null ? "" : user.getAvatarUrl(),
                        "role", user.getRole(),
                        "is_active", user.isActive(),
                        "last_login_at", user.getLastLoginAt() == null ? "" : user.getLastLoginAt(),
                        "created_at", user.getCreatedAt() == null ? "" : user.getCreatedAt()
                )
        ));
    }
}
