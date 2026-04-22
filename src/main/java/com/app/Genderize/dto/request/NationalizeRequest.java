package com.app.Genderize.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class NationalizeRequest {
    private String name;
    private List<Country> country;

    @Data
    public static class Country {
        private String country_id;
        private Double probability;
    }
}
