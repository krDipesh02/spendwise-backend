package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.SaveExpenseRequest;
import com.spendwise.dto.response.ExpenseDto;
import com.spendwise.service.CurrentUserService;
import com.spendwise.dto.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/expenses")
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CurrentUserService currentUserService;

    public ExpenseController(ExpenseService expenseService, CurrentUserService currentUserService) {
        this.expenseService = expenseService;
        this.currentUserService = currentUserService;
    }

    /**
     * Lists expenses for the authenticated user, optionally constrained to a date range.
     *
     * @param from optional inclusive lower bound for {@code spentAt}, formatted as an ISO local date
     * @param to optional inclusive upper bound for {@code spentAt}, formatted as an ISO local date
     * @return expenses for the current user matching the requested filter
     */
    @GetMapping
    public List<ExpenseDto> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Listing expenses for userId={} from={} to={}", user.getId(), from, to);
        return (from != null && to != null)
                ? expenseService.listBetweenDtos(user, from, to)
                : expenseService.listDtos(user);
    }

    /**
     * Retrieves a single expense belonging to the authenticated user.
     *
     * @param id the expense identifier
     * @return the requested expense
     */
    @GetMapping("/{id}")
    public ExpenseDto get(@PathVariable UUID id) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Fetching expense for userId={} expenseId={}", user.getId(), id);
        return expenseService.getDto(user, id);
    }

    /**
     * Creates an expense for the authenticated user.
     *
     * @param request contains the expense details to persist
     * @return the created expense
     */
    @PostMapping
    public ExpenseDto create(@Valid @RequestBody SaveExpenseRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Creating expense for userId={} categoryId={} spentAt={}", user.getId(), request.getCategoryId(), request.getSpentAt());
        return expenseService.createDto(
                user,
                request.getCategoryId(),
                request.getAmount(),
                request.getCurrency(),
                request.getSpentAt(),
                request.getMerchant(),
                request.getDescription(),
                request.getReceiptId()
        );
    }

    /**
     * Updates an existing expense belonging to the authenticated user.
     *
     * @param id the expense identifier
     * @param request contains the replacement expense details
     * @return the updated expense
     */
    @PutMapping("/{id}")
    public ExpenseDto update(@PathVariable UUID id, @Valid @RequestBody SaveExpenseRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Updating expense for userId={} expenseId={}", user.getId(), id);
        return expenseService.updateDto(
                user,
                id,
                request.getCategoryId(),
                request.getAmount(),
                request.getCurrency(),
                request.getSpentAt(),
                request.getMerchant(),
                request.getDescription(),
                request.getReceiptId()
        );
    }

    /**
     * Deletes an expense belonging to the authenticated user.
     *
     * @param id the expense identifier
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Deleting expense for userId={} expenseId={}", user.getId(), id);
        expenseService.delete(user, id);
    }
}
