package com.spendwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank
    private String displayName;

    @NotBlank
    private String baseCurrency;

    @NotBlank
    private String timezone;

    @DecimalMin("0.00")
    private BigDecimal monthlyLimit;
}
