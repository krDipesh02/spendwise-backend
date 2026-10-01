package com.spendwise.dto.response;

import com.spendwise.dto.entity.Reminder;
import com.spendwise.model.ReminderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderDto {

    private String id;
    private ReminderType type;
    private String title;
    private String message;
    private LocalDate dueDate;
    private boolean acknowledged;

    public static ReminderDto from(Reminder reminder) {
        return new ReminderDto(
                reminder.getId().toString(),
                reminder.getType(),
                reminder.getTitle(),
                reminder.getMessage(),
                reminder.getDueDate(),
                reminder.isAcknowledged()
        );
    }
}
