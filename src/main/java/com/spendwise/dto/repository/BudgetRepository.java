package com.spendwise.dto.repository;

import com.spendwise.dto.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndMonthStart(UUID userId, LocalDate monthStart);

    Optional<Budget> findByUserIdAndCategoryIdAndMonthStart(UUID userId, UUID categoryId, LocalDate monthStart);

    Optional<Budget> findByUserIdAndCategoryIsNullAndMonthStart(UUID userId, LocalDate monthStart);
}
