package com.app.Genderize.dto;

import lombok.Data;

import java.util.List;

@Data
public class NationalizeDto {
    private String name;
    private List<Country> country;

    @Data
    public static class Country {
        private String country_id;
        private Double probability;
    }
}
