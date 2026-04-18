package com.app.Genderize.service;

import com.app.Genderize.dto.AgifyDto;
import com.app.Genderize.dto.GenderizeDto;
import com.app.Genderize.dto.NationalizeDto;
import com.app.Genderize.exception.BadCredentialException;
import com.app.Genderize.exception.ExternalApiException;
import com.app.Genderize.model.Profile;
import com.app.Genderize.repository.ProfileRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository repository;
    private final RestTemplate restTemplate;

    private static final String GENDERIZE = "https://api.genderize.io?name=%s";
    private static final String AGIFY = "https://api.agify.io?name=%s";
    private static final String NATIONALIZE = "https://api.nationalize.io?name=%s";

    public Object createProfile(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new BadCredentialException("Missing or empty name");
        }

        String normalized = name.trim().toLowerCase();

        Optional<Profile> existing = repository.findByNameIgnoreCase(normalized);
        if (existing.isPresent()) {
            return Map.of(
                    "status", "success",
                    "message", "Profile already exists",
                    "data", existing.get()
            );
        }

        GenderizeDto g = restTemplate.getForObject(GENDERIZE.formatted(normalized), GenderizeDto.class);
        AgifyDto a = restTemplate.getForObject(AGIFY.formatted(normalized), AgifyDto.class);
        NationalizeDto n = restTemplate.getForObject(NATIONALIZE.formatted(normalized), NationalizeDto.class);

        validateGenderize(g);
        validateAgify(a);
        validateNationalize(n);

        var topCountry = n.getCountry().stream()
                .max(Comparator.comparing(NationalizeDto.Country::getProbability))
                .orElseThrow(() -> new ExternalApiException("Nationalize"));

        Profile profile = Profile.builder()
                .id(UuidCreator.getTimeOrdered()) // replace with UUID v7 if needed
                .name(normalized)
                .gender(g.getGender())
                .genderProbability(g.getProbability())
                .sampleSize(g.getCount())
                .age(a.getAge())
                .ageGroup(classifyAge(a.getAge()))
                .countryId(topCountry.getCountry_id())
                .countryProbability(topCountry.getProbability())
                .createdAt(Instant.now())
                .build();

        repository.save(profile);

        return Map.of(
                "status", "success",
                "data", profile
        );
    }

    private void validateGenderize(GenderizeDto g) {
        if (g == null || g.getGender() == null || g.getCount() == 0) {
            throw new ExternalApiException("Genderize");
        }
    }

    private void validateAgify(AgifyDto a) {
        if (a == null || a.getAge() == null) {
            throw new ExternalApiException("Agify");
        }
    }

    private void validateNationalize(NationalizeDto n) {
        if (n == null || n.getCountry() == null || n.getCountry().isEmpty()) {
            throw new ExternalApiException("Nationalize");
        }
    }

    private String classifyAge(int age) {
        if (age <= 12) return "child";
        if (age <= 19) return "teenager";
        if (age <= 59) return "adult";
        return "senior";
    }

    public Profile getOne(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    public List<Profile> getAll(String gender, String countryId, String ageGroup) {
        return repository.filter(
                like(gender),
                like(countryId),
                like(ageGroup)
        );
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) throw new NoSuchElementException();
        repository.deleteById(id);
    }

    private String like(String v) {
        return v == null ? null : "%" + v.toLowerCase() + "%";
    }
}
