package com.spendwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveExpenseRequest {

    private UUID categoryId;
    private UUID receiptId;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private LocalDate spentAt;

    private String merchant;
    private String description;
}
