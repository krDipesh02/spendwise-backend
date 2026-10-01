package com.spendwise.dto.response;

import com.spendwise.dto.entity.RecurringExpense;
import com.spendwise.model.RecurringFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseDto {

    private String id;
    private String categoryId;
    private String categoryName;
    private String name;
    private BigDecimal amount;
    private String currency;
    private RecurringFrequency frequency;
    private LocalDate nextDueDate;
    private boolean active;

    public static RecurringExpenseDto from(RecurringExpense recurringExpense) {
        return new RecurringExpenseDto(
                recurringExpense.getId().toString(),
                recurringExpense.getCategory() == null ? null : recurringExpense.getCategory().getId().toString(),
                recurringExpense.getCategory() == null ? null : recurringExpense.getCategory().getName(),
                recurringExpense.getName(),
                recurringExpense.getAmount(),
                recurringExpense.getCurrency(),
                recurringExpense.getFrequency(),
                recurringExpense.getNextDueDate(),
                recurringExpense.isActive()
        );
    }
}
