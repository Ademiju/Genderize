package com.app.Genderize.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageResponse<T> {
    private int page;
    private int limit;
    private long total;
    private T data;
}
