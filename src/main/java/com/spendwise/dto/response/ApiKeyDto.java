package com.spendwise.dto.response;

import com.spendwise.dto.entity.UserApiKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDto {

    private String id;
    private String name;
    private String keyPrefix;
    private Instant lastUsedAt;
    private Instant createdAt;

    public static ApiKeyDto from(UserApiKey apiKey) {
        return new ApiKeyDto(
                apiKey.getId().toString(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getLastUsedAt(),
                apiKey.getCreatedAt()
        );
    }
}
