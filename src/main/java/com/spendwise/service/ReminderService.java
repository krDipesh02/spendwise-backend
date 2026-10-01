package com.spendwise.service;

import com.spendwise.dto.entity.RecurringExpense;
import com.spendwise.dto.entity.Reminder;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.ReminderRepository;
import com.spendwise.dto.service.AuditService;
import com.spendwise.dto.service.BudgetService;
import com.spendwise.dto.service.RecurringExpenseService;
import com.spendwise.model.ReminderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final BudgetService budgetService;
    private final RecurringExpenseService recurringExpenseService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;

    public ReminderService(ReminderRepository reminderRepository,
                           BudgetService budgetService,
                           RecurringExpenseService recurringExpenseService,
                           AnalyticsService analyticsService,
                           AuditService auditService) {
        this.reminderRepository = reminderRepository;
        this.budgetService = budgetService;
        this.recurringExpenseService = recurringExpenseService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
    }

    @Transactional
    public List<Reminder> generateReminders(UserProfile user) {
        log.info("Generating reminders for userId={}", user.getId());
        LocalDate today = LocalDate.now();
        List<Reminder> reminders = new ArrayList<>();

        budgetService.getBudgetStatus(user, YearMonth.now()).stream()
                .filter(status -> status.getAmount().signum() > 0)
                .filter(status -> status.getSpent().compareTo(status.getAmount().multiply(java.math.BigDecimal.valueOf(0.8))) >= 0)
                .forEach(status -> reminders.add(createReminder(
                        user,
                        ReminderType.BUDGET_ALERT,
                        "Budget threshold reached",
                        "%s budget is nearing its limit".formatted(status.getCategoryName()),
                        today
                )));

        for (RecurringExpense recurringExpense : recurringExpenseService.dueBy(user, today.plusDays(3))) {
            reminders.add(createReminder(
                    user,
                    ReminderType.RECURRING_DUE,
                    "Recurring expense due",
                    "%s is due on %s".formatted(recurringExpense.getName(), recurringExpense.getNextDueDate()),
                    recurringExpense.getNextDueDate()
            ));
        }

        if (!analyticsService.findUnusualSpend(user, YearMonth.now()).isEmpty()) {
            reminders.add(createReminder(
                    user,
                    ReminderType.BILL_DUE,
                    "Unusual spending detected",
                    "This month includes one or more unusually large expenses",
                    today
            ));
        }

        List<Reminder> saved = reminderRepository.saveAll(reminders);
        log.info("Generated reminders for userId={} count={}", user.getId(), saved.size());
        auditService.log(user, "GENERATE_REMINDERS", "REMINDER", "BATCH", String.valueOf(saved.size()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Reminder> listUpcoming(UserProfile user) {
        log.debug("Listing upcoming reminders for userId={}", user.getId());
        return reminderRepository.findByUserIdAndDueDateGreaterThanEqualOrderByDueDateAsc(user.getId(), LocalDate.now());
    }

    private Reminder createReminder(UserProfile user, ReminderType type, String title, String message, LocalDate dueDate) {
        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setType(type);
        reminder.setTitle(title);
        reminder.setMessage(message);
        reminder.setDueDate(dueDate);
        reminder.setAcknowledged(false);
        return reminder;
    }
}
