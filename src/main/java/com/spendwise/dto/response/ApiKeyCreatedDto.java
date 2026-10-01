package com.spendwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreatedDto {

    private String id;
    private String name;
    private String keyPrefix;
    private String apiKey;
}
