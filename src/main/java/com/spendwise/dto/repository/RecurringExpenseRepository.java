package com.spendwise.dto.repository;

import com.spendwise.dto.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, UUID> {

    List<RecurringExpense> findByUserIdOrderByNextDueDateAsc(UUID userId);

    List<RecurringExpense> findByUserIdAndActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(UUID userId, LocalDate dueDate);

    Optional<RecurringExpense> findByIdAndUserId(UUID id, UUID userId);
}
