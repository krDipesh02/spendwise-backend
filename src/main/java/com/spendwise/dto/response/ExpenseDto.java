package com.spendwise.dto.response;

import com.spendwise.dto.entity.Expense;
import com.spendwise.model.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {

    private String id;
    private String categoryId;
    private String categoryName;
    private String receiptId;
    private BigDecimal amount;
    private String currency;
    private LocalDate spentAt;
    private String merchant;
    private String description;
    private ExpenseStatus status;

    public static ExpenseDto from(Expense expense) {
        return new ExpenseDto(
                expense.getId().toString(),
                expense.getCategory() == null ? null : expense.getCategory().getId().toString(),
                expense.getCategory() == null ? null : expense.getCategory().getName(),
                expense.getReceipt() == null ? null : expense.getReceipt().getId().toString(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getSpentAt(),
                expense.getMerchant(),
                expense.getDescription(),
                expense.getStatus()
        );
    }
}
