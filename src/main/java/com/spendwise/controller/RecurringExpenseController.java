package com.spendwise.controller;

import com.spendwise.dto.entity.RecurringExpense;
import com.spendwise.model.RecurringFrequency;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.SaveRecurringExpenseRequest;
import com.spendwise.dto.response.RecurringExpenseDto;
import com.spendwise.service.CurrentUserService;
import com.spendwise.dto.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/recurring-expenses")
@Slf4j
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    private final CurrentUserService currentUserService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService, CurrentUserService currentUserService) {
        this.recurringExpenseService = recurringExpenseService;
        this.currentUserService = currentUserService;
    }

    /**
     * Creates a recurring expense definition for the authenticated user.
     *
     * @param request contains the recurring expense details, including schedule and category
     * @return the created recurring expense
     */
    @PostMapping
    public RecurringExpenseDto create(@Valid @RequestBody SaveRecurringExpenseRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Creating recurring expense for userId={} categoryId={} nextDueDate={}", user.getId(), request.getCategoryId(), request.getNextDueDate());
        return RecurringExpenseDto.from(recurringExpenseService.create(
                user,
                request.getCategoryId(),
                request.getName(),
                request.getAmount(),
                request.getCurrency(),
                request.getFrequency(),
                request.getNextDueDate()
        ));
    }

    /**
     * Lists recurring expenses for the authenticated user, optionally filtered by due date.
     *
     * @param dueBy optional upper bound for the next due date, formatted as an ISO local date
     * @return recurring expenses matching the requested filter
     */
    @GetMapping
    public List<RecurringExpenseDto> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBy) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Listing recurring expenses for userId={} dueBy={}", user.getId(), dueBy);
        List<RecurringExpense> recurringExpenses = dueBy == null
                ? recurringExpenseService.list(user)
                : recurringExpenseService.dueBy(user, dueBy);
        return recurringExpenses.stream().map(RecurringExpenseDto::from).toList();
    }
}
