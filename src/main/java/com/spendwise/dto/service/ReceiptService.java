package com.spendwise.dto.service;

import com.spendwise.dto.entity.Receipt;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.ReceiptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ReceiptService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+[\\.,]?\\d{0,2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    private final ReceiptRepository receiptRepository;
    private final AuditService auditService;

    public ReceiptService(ReceiptRepository receiptRepository, AuditService auditService) {
        this.receiptRepository = receiptRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Receipt upload(UserProfile user, String fileUrl, String rawText) {
        log.info("Uploading receipt metadata for userId={} fileUrl={}", user.getId(), fileUrl);
        Receipt receipt = new Receipt();
        receipt.setFileUrl(fileUrl);
        receipt.setRawText(rawText);
        applyExtraction(receipt, rawText);
        Receipt saved = receiptRepository.save(receipt);
        log.info("Stored receiptId={} for userId={}", saved.getId(), user.getId());
        auditService.log(user, "UPLOAD_RECEIPT", "RECEIPT", saved.getId().toString(), fileUrl);
        return saved;
    }

    @Transactional(readOnly = true)
    public Receipt get(UUID receiptId) {
        log.debug("Fetching receiptId={}", receiptId);
        return receiptRepository.findById(receiptId).orElseThrow();
    }

    private void applyExtraction(Receipt receipt, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.debug("Skipping receipt extraction because raw text is empty");
            return;
        }
        log.debug("Applying receipt extraction");
        receipt.setExtractedMerchant(rawText.lines().findFirst().orElse("Unknown Merchant"));
        extractAmount(rawText).ifPresent(receipt::setExtractedTotal);
        extractDate(rawText).ifPresent(receipt::setExtractedDate);
    }

    private Optional<BigDecimal> extractAmount(String rawText) {
        Matcher matcher = AMOUNT_PATTERN.matcher(rawText);
        BigDecimal result = null;
        while (matcher.find()) {
            result = new BigDecimal(matcher.group(1).replace(",", ""));
        }
        return Optional.ofNullable(result);
    }

    private Optional<LocalDate> extractDate(String rawText) {
        Matcher matcher = DATE_PATTERN.matcher(rawText);
        if (matcher.find()) {
            return Optional.of(LocalDate.parse(matcher.group(1)));
        }
        return Optional.empty();
    }
}
