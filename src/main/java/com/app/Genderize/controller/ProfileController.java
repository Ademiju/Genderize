package com.app.Genderize.controller;

import com.app.Genderize.dto.response.GenericResponse;
import com.app.Genderize.dto.response.PageResponse;
import com.app.Genderize.model.Profile;
import com.app.Genderize.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Object response = service.createProfile(body.get("name"));

        if (response instanceof Map map && map.containsKey("message")) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("hasAnyRole('admin','analyst')")
    @GetMapping("/{id}")
    public GenericResponse<?> getOne(@PathVariable UUID id) {
        return GenericResponse.builder()
                .status("success")
                .data(service.getOne(id))
                .build();
    }

    @PreAuthorize("hasAnyRole('admin','analyst')")
    @GetMapping
    public GenericResponse<?> getAll(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age_group,
            @RequestParam(required = false) String country_id,
            @RequestParam(required = false) Integer min_age,
            @RequestParam(required = false) Integer max_age,
            @RequestParam(required = false) Double min_gender_probability,
            @RequestParam(required = false) Double min_country_probability,
            @RequestParam(required = false) String sort_by,
            @RequestParam(required = false) String order,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request
    ) {
        PageResponse<Object> result = service.getAll(
                gender,
                age_group,
                country_id,
                min_age,
                max_age,
                min_gender_probability,
                min_country_probability,
                sort_by,
                order,
                page,
                limit
        );

        return GenericResponse.builder()
                .status("success")
                .page(result.getPage())
                .limit(result.getLimit())
                .total(result.getTotal())
                .totalPages(result.getTotalPages())
                .links(buildLinks(request, result.getPage(), result.getLimit(), result.getTotalPages()))
                .data(result.getData())
                .build();
    }

    @PreAuthorize("hasAnyRole('admin','analyst')")
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request
    ) {
        ResponseEntity<?> response = service.searchProfiles(query, page, limit);
        Object body = response.getBody();
        if (body instanceof GenericResponse<?> generic && "success".equals(generic.getStatus())) {
            generic.setLinks(buildLinks(request, generic.getPage(), generic.getLimit(), generic.getTotalPages()));
        }
        return response;

    }

    @PreAuthorize("hasAnyRole('admin','analyst')")
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age_group,
            @RequestParam(required = false) String country_id,
            @RequestParam(required = false) Integer min_age,
            @RequestParam(required = false) Integer max_age,
            @RequestParam(required = false) Double min_gender_probability,
            @RequestParam(required = false) Double min_country_probability,
            @RequestParam(required = false) String sort_by,
            @RequestParam(required = false) String order
    ) {
        if (!"csv".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("status,message\nerror,Only csv export is supported\n");
        }

        List<Profile> profiles = service.exportProfiles(
                gender,
                age_group,
                country_id,
                min_age,
                max_age,
                min_gender_probability,
                min_country_probability,
                sort_by,
                order
        );

        String filename = "profiles_" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(toCsv(profiles));
    }

    @PreAuthorize("hasAnyRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, String> buildLinks(HttpServletRequest request, Integer page, Integer limit, Integer totalPages) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", pageLink(request, page, limit));
        links.put("next", page < totalPages ? pageLink(request, page + 1, limit) : null);
        links.put("prev", page > 1 ? pageLink(request, page - 1, limit) : null);
        return links;
    }

    private String pageLink(HttpServletRequest request, int page, int limit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());
        request.getParameterMap().forEach((key, values) -> {
            if (!"page".equals(key) && !"limit".equals(key)) {
                for (String value : values) {
                    builder.queryParam(key, value);
                }
            }
        });
        return builder
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build()
                .toUriString();
    }

    private String toCsv(List<Profile> profiles) {
        StringBuilder csv = new StringBuilder("id,name,gender,gender_probability,age,age_group,country_id,country_name,country_probability,created_at\n");
        for (Profile profile : profiles) {
            csv.append(csv(profile.getId()))
                    .append(',').append(csv(profile.getName()))
                    .append(',').append(csv(profile.getGender()))
                    .append(',').append(profile.getGenderProbability())
                    .append(',').append(profile.getAge())
                    .append(',').append(csv(profile.getAgeGroup()))
                    .append(',').append(csv(profile.getCountryId()))
                    .append(',').append(csv(profile.getCountryName()))
                    .append(',').append(profile.getCountryProbability())
                    .append(',').append(csv(profile.getCreatedAt()))
                    .append('\n');
        }
        return csv.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
