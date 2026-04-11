package com.app.Genderize.controller;

import com.app.Genderize.response.GenericResponse;
import com.app.Genderize.service.GenderizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class GenderizeController {

    private final GenderizeService service;

    @GetMapping(value = "classify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse<Object>> classify(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(GenericResponse.builder().status("error").message("Name parameter is required but is empty").build());
        }
        return service.classifyGenderByName(name);
    }
}
