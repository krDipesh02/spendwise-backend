package com.spendwise.repository;

import com.spendwise.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByUserIdAndDueDateGreaterThanEqualOrderByDueDateAsc(UUID userId, LocalDate dueDate);
}
