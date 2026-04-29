package com.app.Genderize.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
public class RefreshToken {
    @Id
    private UUID id;

    private UUID userId;

    private String tokenHash;

    private Instant expiresAt;

    private boolean revoked = false;
}
