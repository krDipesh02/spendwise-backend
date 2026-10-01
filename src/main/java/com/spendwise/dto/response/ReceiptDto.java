package com.spendwise.dto.response;

import com.spendwise.dto.entity.Receipt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDto {

    private String id;
    private String fileUrl;
    private String rawText;
    private String extractedMerchant;
    private LocalDate extractedDate;
    private BigDecimal extractedTotal;

    public static ReceiptDto from(Receipt receipt) {
        return new ReceiptDto(
                receipt.getId().toString(),
                receipt.getFileUrl(),
                receipt.getRawText(),
                receipt.getExtractedMerchant(),
                receipt.getExtractedDate(),
                receipt.getExtractedTotal()
        );
    }
}
