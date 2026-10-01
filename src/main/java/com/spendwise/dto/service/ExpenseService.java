package com.spendwise.dto.service;

import com.spendwise.dto.entity.Category;
import com.spendwise.dto.entity.Expense;
import com.spendwise.dto.entity.Receipt;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.CategoryRepository;
import com.spendwise.dto.repository.ExpenseRepository;
import com.spendwise.dto.repository.ReceiptRepository;
import com.spendwise.dto.response.ExpenseDto;
import com.spendwise.model.ExpenseStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ReceiptRepository receiptRepository;
    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ReceiptRepository receiptRepository,
                          CategoryRepository categoryRepository,
                          AuditService auditService) {
        this.expenseRepository = expenseRepository;
        this.receiptRepository = receiptRepository;
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Expense create(UserProfile user,
                          UUID categoryId,
                          BigDecimal amount,
                          String currency,
                          LocalDate spentAt,
                          String merchant,
                          String description,
                          UUID receiptId) {
        log.info("Creating expense for userId={} categoryId={} spentAt={} receiptId={}",
                user.getId(), categoryId, spentAt, receiptId);
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(resolveCategory(user, categoryId));
        expense.setReceipt(resolveReceipt(receiptId));
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setSpentAt(spentAt);
        expense.setMerchant(merchant);
        expense.setDescription(description);
        expense.setStatus(ExpenseStatus.RECORDED);
        Expense saved = expenseRepository.save(expense);
        log.info("Created expenseId={} for userId={}", saved.getId(), user.getId());
        auditService.log(user, "CREATE_EXPENSE", "EXPENSE", saved.getId().toString(), saved.getAmount().toPlainString());
        return saved;
    }

    @Transactional
    public ExpenseDto createDto(UserProfile user,
                                UUID categoryId,
                                BigDecimal amount,
                                String currency,
                                LocalDate spentAt,
                                String merchant,
                                String description,
                                UUID receiptId) {
        return ExpenseDto.from(create(user, categoryId, amount, currency, spentAt, merchant, description, receiptId));
    }

    @Transactional
    public Expense update(UserProfile user,
                          UUID expenseId,
                          UUID categoryId,
                          BigDecimal amount,
                          String currency,
                          LocalDate spentAt,
                          String merchant,
                          String description,
                          UUID receiptId) {
        log.info("Updating expenseId={} for userId={}", expenseId, user.getId());
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        expense.setCategory(resolveCategory(user, categoryId));
        expense.setReceipt(resolveReceipt(receiptId));
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setSpentAt(spentAt);
        expense.setMerchant(merchant);
        expense.setDescription(description);
        log.info("Updated expenseId={} for userId={}", expense.getId(), user.getId());
        auditService.log(user, "UPDATE_EXPENSE", "EXPENSE", expense.getId().toString(), expense.getAmount().toPlainString());
        return expense;
    }

    @Transactional
    public ExpenseDto updateDto(UserProfile user,
                                UUID expenseId,
                                UUID categoryId,
                                BigDecimal amount,
                                String currency,
                                LocalDate spentAt,
                                String merchant,
                                String description,
                                UUID receiptId) {
        return ExpenseDto.from(update(user, expenseId, categoryId, amount, currency, spentAt, merchant, description, receiptId));
    }

    @Transactional
    public void delete(UserProfile user, UUID expenseId) {
        log.info("Deleting expenseId={} for userId={}", expenseId, user.getId());
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
        log.info("Deleted expenseId={} for userId={}", expenseId, user.getId());
        auditService.log(user, "DELETE_EXPENSE", "EXPENSE", expenseId.toString(), expense.getAmount().toPlainString());
    }

    @Transactional(readOnly = true)
    public List<Expense> list(UserProfile user) {
        log.debug("Listing all expenses for userId={}", user.getId());
        return expenseRepository.findByUserIdOrderBySpentAtDescCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> listDtos(UserProfile user) {
        return list(user).stream().map(ExpenseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Expense> listBetween(UserProfile user, LocalDate from, LocalDate to) {
        log.debug("Listing expenses between dates for userId={} from={} to={}", user.getId(), from, to);
        return expenseRepository.findByUserIdAndSpentAtBetweenOrderBySpentAtAsc(user.getId(), from, to);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> listBetweenDtos(UserProfile user, LocalDate from, LocalDate to) {
        return listBetween(user, from, to).stream().map(ExpenseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Expense get(UserProfile user, UUID expenseId) {
        log.debug("Fetching expenseId={} for userId={}", expenseId, user.getId());
        return expenseRepository.findByIdAndUserId(expenseId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
    }

    @Transactional(readOnly = true)
    public ExpenseDto getDto(UserProfile user, UUID expenseId) {
        return ExpenseDto.from(get(user, expenseId));
    }

    private Category resolveCategory(UserProfile user, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        log.debug("Resolving expense categoryId={} for userId={}", categoryId, user.getId());
        return categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    private Receipt resolveReceipt(UUID receiptId) {
        if (receiptId == null) {
            return null;
        }
        log.debug("Resolving receiptId={} for expense mutation", receiptId);
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found"));
    }
}
