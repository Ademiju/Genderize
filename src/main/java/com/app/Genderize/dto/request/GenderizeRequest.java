package com.app.Genderize.dto.request;

import lombok.Data;

@Data
public class GenderizeRequest {
    private String name;
    private String gender;
    private Double probability;
    private Integer count;
}
