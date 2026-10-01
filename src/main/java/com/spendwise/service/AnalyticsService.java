package com.spendwise.service;

import com.spendwise.dto.entity.Expense;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.service.ExpenseService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalyticsService {

    private final ExpenseService expenseService;

    public AnalyticsService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public MonthlySummary getMonthlySummary(UserProfile user, YearMonth month) {
        log.debug("Calculating monthly summary for userId={} month={}", user.getId(), month);
        List<Expense> expenses = expenseService.listBetween(user, month.atDay(1), month.atEndOfMonth());
        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = expenses.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
        return new MonthlySummary(month, expenses.size(), total, average);
    }

    @Transactional(readOnly = true)
    public List<CategorySpend> getCategorySummary(UserProfile user, YearMonth month) {
        log.debug("Calculating category summary for userId={} month={}", user.getId(), month);
        return expenseService.listBetween(user, month.atDay(1), month.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory() == null ? "UNCATEGORIZED" : expense.getCategory().getName(),
                        Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .entrySet()
                .stream()
                .map(entry -> new CategorySpend(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategorySpend::getTotal).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> getSpendingTrend(UserProfile user, LocalDate from, LocalDate to) {
        log.debug("Calculating spending trend for userId={} from={} to={}", user.getId(), from, to);
        Map<LocalDate, BigDecimal> trend = expenseService.listBetween(user, from, to).stream()
                .collect(Collectors.groupingBy(
                        Expense::getSpentAt,
                        Collectors.mapping(Expense::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
        return trend.entrySet().stream()
                .map(entry -> new TrendPoint(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TrendPoint::getDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseOutlier> findUnusualSpend(UserProfile user, YearMonth month) {
        log.debug("Finding unusual spend for userId={} month={}", user.getId(), month);
        List<Expense> expenses = expenseService.listBetween(user, month.atDay(1), month.atEndOfMonth());
        BigDecimal average = getMonthlySummary(user, month).getAverageSpend();
        BigDecimal threshold = average.multiply(BigDecimal.valueOf(2));
        return expenses.stream()
                .filter(expense -> expense.getAmount().compareTo(threshold) > 0)
                .map(expense -> new ExpenseOutlier(
                        expense.getId().toString(),
                        expense.getAmount(),
                        expense.getMerchant(),
                        expense.getSpentAt(),
                        threshold
                ))
                .toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySummary {

        private YearMonth month;
        private int expenseCount;
        private BigDecimal totalSpend;
        private BigDecimal averageSpend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySpend {

        private String categoryName;
        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {

        private LocalDate date;
        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseOutlier {

        private String expenseId;
        private BigDecimal amount;
        private String merchant;
        private LocalDate spentAt;
        private BigDecimal threshold;
    }
}
