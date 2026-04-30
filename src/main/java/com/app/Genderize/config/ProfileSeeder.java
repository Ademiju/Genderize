package com.app.Genderize.config;

import com.app.Genderize.model.Profile;
import com.app.Genderize.repository.ProfileRepository;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileSeeder implements CommandLineRunner {

    private static final String FILE_SYSTEM_SEED = "file:seed_profile.json";
    private static final String CLASSPATH_SEED = "classpath:seed_profile.json";
    private static final String LEGACY_CLASSPATH_SEED = "classpath:seed_profiles.json";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Resource seedResource = resolveSeedResource();
        if (seedResource == null) {
            log.warn("Profile seed skipped: no seed_profile.json or seed_profiles.json file was found");
            return;
        }

        SeedFile seedFile = readSeedFile(seedResource);
        if (seedFile.profiles() == null || seedFile.profiles().isEmpty()) {
            log.info("Profile seed skipped: {} contains no profiles", seedResource.getFilename());
            return;
        }

        Map<String, Profile> existingProfilesByName = profileRepository.findAll().stream()
                .filter(profile -> profile.getName() != null && !profile.getName().isBlank())
                .collect(Collectors.toMap(
                        profile -> normalizeName(profile.getName()),
                        Function.identity(),
                        (first, second) -> first
                ));
        List<Profile> profilesToSave = new ArrayList<>();
        int insertedCount = 0;
        int updatedCount = 0;

        for (SeedProfile seedProfile : seedFile.profiles()) {
            String normalizedName = normalizeName(seedProfile.name());
            if (normalizedName == null) {
                continue;
            }

            Profile existingProfile = existingProfilesByName.get(normalizedName);
            boolean isUpdate = existingProfile != null;

            profilesToSave.add(Profile.builder()
                    .id(isUpdate ? existingProfile.getId() : UuidCreator.getTimeOrdered())
                    .name(normalizedName)
                    .gender(seedProfile.gender())
                    .genderProbability(seedProfile.genderProbability() == null ? 0.0 : seedProfile.genderProbability())
                    .sampleSize(0)
                    .age(seedProfile.age())
                    .ageGroup(seedProfile.ageGroup())
                    .countryId(seedProfile.countryId())
                    .countryProbability(seedProfile.countryProbability() == null ? 0.0 : seedProfile.countryProbability())
                    .createdAt(isUpdate ? existingProfile.getCreatedAt() : Instant.now())
                    .build());

            if (isUpdate) {
                updatedCount++;
            } else {
                insertedCount++;
            }
        }

        if (profilesToSave.isEmpty()) {
            log.info("Profile seed skipped: {} contains no valid profiles", seedResource.getFilename());
            return;
        }

        profileRepository.saveAll(profilesToSave);
        log.info(
                "Profile seed completed: inserted {} and updated {} profiles from {}",
                insertedCount,
                updatedCount,
                seedResource.getFilename()
        );
    }

    private Resource resolveSeedResource() {
        for (String location : List.of(FILE_SYSTEM_SEED, CLASSPATH_SEED, LEGACY_CLASSPATH_SEED)) {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                return resource;
            }
        }
        return null;
    }

    private SeedFile readSeedFile(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, SeedFile.class);
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedFile(List<SeedProfile> profiles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SeedProfile(
            String name,
            String gender,
            Double genderProbability,
//            @JsonAlias("count")
//            Integer sampleSize,
            Integer age,
            String ageGroup,
            String countryId,
            Double countryProbability
    ) {
    }
}
