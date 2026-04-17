package com.app.Genderize.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Profile {

    @Id
    private UUID id;

    private String name;

    private String gender;
    private double genderProbability;
    private int sampleSize;

    private Integer age;
    private String ageGroup;

    private String countryId;
    private double countryProbability;

    private Instant createdAt;
}
