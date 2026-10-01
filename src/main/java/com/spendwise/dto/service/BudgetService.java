package com.spendwise.dto.service;

import com.spendwise.dto.entity.Budget;
import com.spendwise.dto.entity.Category;
import com.spendwise.dto.entity.Expense;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.BudgetRepository;
import com.spendwise.dto.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseService expenseService;
    private final AuditService auditService;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         ExpenseService expenseService,
                         AuditService auditService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.expenseService = expenseService;
        this.auditService = auditService;
    }

    @Transactional
    public Budget setBudget(UserProfile user, UUID categoryId, YearMonth month, BigDecimal amount) {
        log.info("Persisting budget for userId={} categoryId={} month={} amount={}", user.getId(), categoryId, month, amount);
        LocalDate monthStart = month.atDay(1);
        Budget budget = (categoryId == null)
                ? budgetRepository.findByUserIdAndCategoryIsNullAndMonthStart(user.getId(), monthStart).orElseGet(Budget::new)
                : budgetRepository.findByUserIdAndCategoryIdAndMonthStart(user.getId(), categoryId, monthStart).orElseGet(Budget::new);
        budget.setUser(user);
        budget.setCategory(resolveCategory(user, categoryId));
        budget.setMonthStart(monthStart);
        budget.setAmount(amount);
        Budget saved = budgetRepository.save(budget);
        log.info("Persisted budgetId={} for userId={}", saved.getId(), user.getId());
        auditService.log(user, "SET_BUDGET", "BUDGET", saved.getId().toString(), amount.toPlainString());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BudgetStatus> getBudgetStatus(UserProfile user, YearMonth month) {
        log.debug("Computing budget status for userId={} month={}", user.getId(), month);
        LocalDate monthStart = month.atDay(1);
        List<Expense> expenses = expenseService.listBetween(user, month.atDay(1), month.atEndOfMonth());
        return budgetRepository.findByUserIdAndMonthStart(user.getId(), monthStart).stream()
                .map(budget -> toStatus(budget, expenses))
                .toList();
    }

    private BudgetStatus toStatus(Budget budget, List<Expense> expenses) {
        BigDecimal spent = expenses.stream()
                .filter(expense -> {
                    if (budget.getCategory() == null) {
                        return true;
                    }
                    return expense.getCategory() != null && expense.getCategory().getId().equals(budget.getCategory().getId());
                })
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = budget.getAmount().subtract(spent);
        return new BudgetStatus(
                budget.getId().toString(),
                budget.getCategory() == null ? null : budget.getCategory().getId().toString(),
                budget.getCategory() == null ? "OVERALL" : budget.getCategory().getName(),
                budget.getMonthStart(),
                budget.getAmount(),
                spent,
                remaining
        );
    }

    private Category resolveCategory(UserProfile user, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        log.debug("Resolving budget categoryId={} for userId={}", categoryId, user.getId());
        return categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetStatus {

        private String budgetId;
        private String categoryId;
        private String categoryName;
        private LocalDate monthStart;
        private BigDecimal amount;
        private BigDecimal spent;
        private BigDecimal remaining;
    }
}
