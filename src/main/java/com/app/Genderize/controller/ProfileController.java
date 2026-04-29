package com.app.Genderize.controller;

import com.app.Genderize.dto.response.GenericResponse;
import com.app.Genderize.dto.response.PageResponse;
import com.app.Genderize.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "10") Integer limit
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
                .data(result.getData())
                .build();
    }

    @PreAuthorize("hasAnyRole('admin','analyst')")
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return service.searchProfiles(query, page, limit);

    }

    @PreAuthorize("hasAnyRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
