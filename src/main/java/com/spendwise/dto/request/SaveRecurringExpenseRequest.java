package com.spendwise.dto.request;

import com.spendwise.model.RecurringFrequency;
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
public class SaveRecurringExpenseRequest {

    private UUID categoryId;

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private RecurringFrequency frequency;

    @NotNull
    private LocalDate nextDueDate;
}
