package com.spendwise.controller;

import com.spendwise.dto.response.ReminderDto;
import com.spendwise.service.CurrentUserService;
import com.spendwise.service.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reminders")
@Slf4j
public class ReminderController {

    private final ReminderService reminderService;
    private final CurrentUserService currentUserService;

    public ReminderController(ReminderService reminderService, CurrentUserService currentUserService) {
        this.reminderService = reminderService;
        this.currentUserService = currentUserService;
    }

    /**
     * Generates reminder records for the authenticated user based on current expense state.
     *
     * @return the reminders generated for the current request
     */
    @PostMapping("/generate")
    public List<ReminderDto> generate() {
        var user = currentUserService.getCurrentUser();
        log.info("Generating reminders for userId={}", user.getId());
        return reminderService.generateReminders(user).stream().map(ReminderDto::from).toList();
    }

    /**
     * Lists upcoming reminders for the authenticated user.
     *
     * @return upcoming reminders visible to the current user
     */
    @GetMapping
    public List<ReminderDto> list() {
        var user = currentUserService.getCurrentUser();
        log.info("Listing reminders for userId={}", user.getId());
        return reminderService.listUpcoming(user).stream().map(ReminderDto::from).toList();
    }
}
