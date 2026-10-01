package com.spendwise.dto.entity;

import com.spendwise.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receipts")
public class Receipt extends BaseEntity {

    @Column(nullable = false)
    private String fileUrl;

    @Column(length = 4000)
    private String rawText;

    private String extractedMerchant;

    private LocalDate extractedDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal extractedTotal;

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getExtractedMerchant() {
        return extractedMerchant;
    }

    public void setExtractedMerchant(String extractedMerchant) {
        this.extractedMerchant = extractedMerchant;
    }

    public LocalDate getExtractedDate() {
        return extractedDate;
    }

    public void setExtractedDate(LocalDate extractedDate) {
        this.extractedDate = extractedDate;
    }

    public BigDecimal getExtractedTotal() {
        return extractedTotal;
    }

    public void setExtractedTotal(BigDecimal extractedTotal) {
        this.extractedTotal = extractedTotal;
    }
}
