package com.app.Genderize.service;

import com.app.Genderize.dto.request.AgifyRequest;
import com.app.Genderize.dto.request.GenderizeRequest;
import com.app.Genderize.dto.request.NationalizeRequest;
import com.app.Genderize.exception.BadCredentialException;
import com.app.Genderize.exception.ExternalApiException;
import com.app.Genderize.model.Profile;
import com.app.Genderize.repository.ProfileRepository;
import com.app.Genderize.dto.response.GenericResponse;
import com.app.Genderize.dto.response.PageResponse;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    private final ProfileRepository repository;
    private final RestTemplate restTemplate;

    private static final String GENDERIZE = "https://api.genderize.io?name=%s";
    private static final String AGIFY = "https://api.agify.io?name=%s";
    private static final String NATIONALIZE = "https://api.nationalize.io?name=%s";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("age", "created_at", "gender_probability");
    private static final Set<String> ALLOWED_SORT_ORDERS = Set.of("asc", "desc");
    private static final Pattern ABOVE_AGE_PATTERN = Pattern.compile("\\b(?:above|over|older than)\\s+(\\d+)\\b");
    private static final Pattern BELOW_AGE_PATTERN = Pattern.compile("\\b(?:below|under|younger than)\\s+(\\d+)\\b");
    private static final Map<String, String> COUNTRY_NAME_TO_CODE = buildCountryLookup();

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

        GenderizeRequest genderizeRequest = restTemplate.getForObject(GENDERIZE.formatted(normalized), GenderizeRequest.class);
        AgifyRequest agifyRequest = restTemplate.getForObject(AGIFY.formatted(normalized), AgifyRequest.class);
        NationalizeRequest nationalizeRequest = restTemplate.getForObject(NATIONALIZE.formatted(normalized), NationalizeRequest.class);

        validateGenderize(genderizeRequest);
        validateAgify(agifyRequest);
        validateNationalize(nationalizeRequest);

        var topCountry = nationalizeRequest.getCountry().stream()
                .max(Comparator.comparing(NationalizeRequest.Country::getProbability))
                .orElseThrow(() -> new ExternalApiException("Nationalize"));

        Profile profile = Profile.builder()
                .id(UuidCreator.getTimeOrdered()) // replace with UUID v7 if needed
                .name(normalized)
                .gender(genderizeRequest.getGender())
                .genderProbability(genderizeRequest.getProbability())
//                .sampleSize(genderizeDto.getCount())
                .age(agifyRequest.getAge())
                .ageGroup(classifyAge(agifyRequest.getAge()))
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

    private void validateGenderize(GenderizeRequest genderizeRequest) {
        if (genderizeRequest == null || genderizeRequest.getGender() == null || genderizeRequest.getCount() == 0) {
            throw new ExternalApiException("Genderize error");
        }
    }

    private void validateAgify(AgifyRequest a) {
        if (a == null || a.getAge() == null) {
            throw new ExternalApiException("Agify error");
        }
    }

    private void validateNationalize(NationalizeRequest nationalizeRequest) {
        if (nationalizeRequest == null || nationalizeRequest.getCountry() == null || nationalizeRequest.getCountry().isEmpty()) {
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
        return repository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    public PageResponse<Object> getAll(
            String gender,
            String ageGroup,
            String countryId,
            Integer minAge,
            Integer maxAge,
            Double minGenderProbability,
            Double minCountryProbability,
            String sortBy,
            String order,
            Integer page,
            Integer limit
    ) {
        int resolvedPage = page == null ? 1 : page;
        int resolvedLimit = limit == null ? 10 : limit;

        if (resolvedPage < 1) {
            throw new BadCredentialException("page must be greater than or equal to 1");
        }
        if (resolvedLimit < 1 || resolvedLimit > 50) {
            throw new BadCredentialException("limit must be between 1 and 50");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new BadCredentialException("min_age cannot be greater than max_age");
        }

        String resolvedSortBy = sortBy == null || sortBy.isBlank() ? "created_at" : sortBy.trim().toLowerCase(Locale.ROOT);
        String resolvedOrder = order == null || order.isBlank() ? "desc" : order.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new BadCredentialException("sort_by must be one of age, created_at, gender_probability");
        }
        if (!ALLOWED_SORT_ORDERS.contains(resolvedOrder)) {
            throw new BadCredentialException("order must be asc or desc");
        }

        Pageable pageable = PageRequest.of(
                resolvedPage - 1,
                resolvedLimit,
                Sort.by("asc".equals(resolvedOrder) ? Sort.Direction.ASC : Sort.Direction.DESC, mapSortField(resolvedSortBy))
        );

        Specification<Profile> specification = Specification.allOf(equalsIgnoreCase("gender", gender))
                .and(equalsIgnoreCase("ageGroup", ageGroup))
                .and(equalsIgnoreCase("countryId", countryId))
                .and(greaterThanOrEqualTo("age", minAge))
                .and(lessThanOrEqualTo("age", maxAge))
                .and(greaterThanOrEqualTo("genderProbability", minGenderProbability))
                .and(greaterThanOrEqualTo("countryProbability", minCountryProbability));

        Page<Profile> result = repository.findAll(specification, pageable);

        return PageResponse.builder()
        .page(resolvedPage)
        .limit(resolvedLimit)
        .total(result.getTotalElements())
        .data(result.getContent()).build();
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) throw new NoSuchElementException();
        repository.deleteById(id);
    }

    public ResponseEntity<GenericResponse<Object>> searchProfiles(String query, Integer page, Integer limit) {
        SearchCriteria criteria = parseNaturalLanguageQuery(query);
        if (!criteria.isInterpretable()) {
            return ResponseEntity.badRequest().body(GenericResponse.builder()
                            .status("error")
                            .message("Unable to interpret query").build());
        }

        log.info("Natural Language Query Result: {} ", criteria);
        PageResponse<Object> response = getAll(criteria.gender(),
                criteria.ageGroup(),
                criteria.countryId(),
                criteria.minAge(),
                criteria.maxAge(),
                null,
                null,
                "created_at",
                "desc",
                page,
                limit
        );
        return ResponseEntity.ok(GenericResponse.builder()
                .status("success")
                .page(response.getPage())
                .limit(response.getLimit())
                .total(response.getTotal())
                .data(response.getData())
                .build());
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "age" -> "age";
            case "gender_probability" -> "genderProbability";
            default -> "createdAt";
        };
    }

    private Specification<Profile> equalsIgnoreCase(String fieldName, String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null || value.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get(fieldName)),
                    value.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    private <T extends Comparable<? super T>> Specification<Profile> greaterThanOrEqualTo(String fieldName, T value) {
        return (root, query, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    private <T extends Comparable<? super T>> Specification<Profile> lessThanOrEqualTo(String fieldName, T value) {
        return (root, query, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
    }

    private SearchCriteria parseNaturalLanguageQuery(String query) {
        if (query == null || query.isBlank()) {
            return SearchCriteria.empty();
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT).replaceAll(",", " ").replaceAll("\\s+", " ");
        SearchCriteria criteria = SearchCriteria.empty();

        boolean hasMale = containsWord(normalized, "male") || containsWord(normalized, "males");
        boolean hasFemale = containsWord(normalized, "female") || containsWord(normalized, "females");
        if (hasMale ^ hasFemale) {
            criteria = criteria.withGender(hasMale ? "male" : "female");
        }

        if (containsAny(normalized, "teenager", "teenagers", "teen")) {
            criteria = criteria.withAgeGroup("teenager");
        } else if (containsAny(normalized, "adult", "adults")) {
            criteria = criteria.withAgeGroup("adult");
        } else if (containsAny(normalized, "senior", "seniors", "elderly")) {
            criteria = criteria.withAgeGroup("senior");
        } else if (containsAny(normalized, "child", "children", "kid", "kids")) {
            criteria = criteria.withAgeGroup("child");
        }

        if (containsWord(normalized, "young")) {
            criteria = criteria.withMinAge(16).withMaxAge(24);
        }

        Matcher aboveMatcher = ABOVE_AGE_PATTERN.matcher(normalized);
        if (aboveMatcher.find()) {
            criteria = criteria.withMinAge(Integer.parseInt(aboveMatcher.group(1)));
        }

        Matcher belowMatcher = BELOW_AGE_PATTERN.matcher(normalized);
        if (belowMatcher.find()) {
            criteria = criteria.withMaxAge(Integer.parseInt(belowMatcher.group(1)));
        }

        String countryCode = extractCountryCode(normalized);
        if (countryCode != null) {
            criteria = criteria.withCountryId(countryCode);
        }

        return criteria;
    }

    private boolean containsWord(String source, String word) {
        return source.matches(".*\\b" + Pattern.quote(word) + "\\b.*");
    }

    private boolean containsAny(String source, String... words) {
        for (String word : words) {
            if (containsWord(source, word)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> buildCountryLookup() {
        Map<String, String> countries = new HashMap<>();
        for (String isoCountry : Locale.getISOCountries()) {
            Locale locale = Locale.of("", isoCountry);
            countries.put(locale.getDisplayCountry(Locale.ENGLISH).toLowerCase(Locale.ROOT), isoCountry);
        }
        countries.put("usa", "US");
        countries.put("us", "US");
        countries.put("uk", "GB");
        countries.put("dr congo", "CD");
        countries.put("congo", "CG");
        return countries;
    }

    private String extractCountryCode(String normalizedQuery) {
        int fromIndex = normalizedQuery.indexOf("from ");
        if (fromIndex < 0) {
            return null;
        }

        String countrySegment = normalizedQuery.substring(fromIndex + 5).trim();
        return COUNTRY_NAME_TO_CODE.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()))
                .filter(entry -> countrySegment.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private record SearchCriteria(
            String gender,
            String ageGroup,
            String countryId,
            Integer minAge,
            Integer maxAge
    ) {
        private static SearchCriteria empty() {
            return new SearchCriteria(null, null, null, null, null);
        }

        private boolean isInterpretable() {
            return gender != null || ageGroup != null || countryId != null || minAge != null || maxAge != null;
        }

        private SearchCriteria withGender(String value) {
            return new SearchCriteria(value, ageGroup, countryId, minAge, maxAge);
        }

        private SearchCriteria withAgeGroup(String value) {
            return new SearchCriteria(gender, value, countryId, minAge, maxAge);
        }

        private SearchCriteria withCountryId(String value) {
            return new SearchCriteria(gender, ageGroup, value, minAge, maxAge);
        }

        private SearchCriteria withMinAge(Integer value) {
            return new SearchCriteria(gender, ageGroup, countryId, value, maxAge);
        }

        private SearchCriteria withMaxAge(Integer value) {
            return new SearchCriteria(gender, ageGroup, countryId, minAge, value);
        }
    }
}
