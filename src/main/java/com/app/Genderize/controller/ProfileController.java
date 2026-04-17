package com.app.Genderize.controller;

import com.app.Genderize.model.Profile;
import com.app.Genderize.response.GenericResponse;
import com.app.Genderize.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Object response = service.createProfile(body.get("name"));

        if (response instanceof Map map && map.containsKey("message")) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public GenericResponse<?> getOne(@PathVariable UUID id) {
        return GenericResponse.builder()
                .status("success")
                .data(service.getOne(id))
                .build();
    }

    @GetMapping
    public GenericResponse<?> getAll(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String country_id,
            @RequestParam(required = false) String age_group
    ) {
        List<Profile> list = service.getAll(gender, country_id, age_group);

        return GenericResponse.builder()
                .status("success")
                .data(list)
                .message("count: " + list.size())
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
