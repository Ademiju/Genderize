package com.app.Genderize.dto;

import lombok.Data;

@Data
public class GenderizeDto {
    private String name;
    private String gender;
    private Double probability;
    private Integer count;
}
