package com.spendwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetBudgetRequest {

    private UUID categoryId;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth month;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;
}
