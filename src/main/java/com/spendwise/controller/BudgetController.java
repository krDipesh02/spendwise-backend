package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.SetBudgetRequest;
import com.spendwise.dto.service.BudgetService;
import com.spendwise.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/budgets")
@Slf4j
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserService currentUserService;

    public BudgetController(BudgetService budgetService, CurrentUserService currentUserService) {
        this.budgetService = budgetService;
        this.currentUserService = currentUserService;
    }

    /**
     * Creates or updates a monthly budget for the authenticated user and returns the resulting budget status.
     *
     * @param request contains the month, optional category, and budget amount to store
     * @return the computed budget status for the updated overall or category budget
     */
    @PostMapping
    public BudgetService.BudgetStatus setBudget(@Valid @RequestBody SetBudgetRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Setting budget for userId={} month={} categoryId={}", user.getId(), request.getMonth(), request.getCategoryId());
        budgetService.setBudget(user, request.getCategoryId(), request.getMonth(), request.getAmount());
        return budgetService.getBudgetStatus(user, request.getMonth()).stream()
                .filter(status -> request.getCategoryId() == null
                        ? "OVERALL".equals(status.getCategoryName())
                        : request.getCategoryId().toString().equals(status.getCategoryId()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Returns the authenticated user's budget status entries for a given month.
     *
     * @param month the month to summarize, formatted as {@code yyyy-MM}
     * @return all budget status entries for the requested month
     */
    @GetMapping
    public List<BudgetService.BudgetStatus> status(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Listing budgets for userId={} month={}", user.getId(), month);
        return budgetService.getBudgetStatus(user, month);
    }
}
