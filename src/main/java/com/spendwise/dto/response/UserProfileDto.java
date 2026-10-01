package com.spendwise.dto.response;

import com.spendwise.dto.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String id;
    private String telegramId;
    private String displayName;
    private String email;
    private boolean emailVerified;
    private boolean googleLinked;
    private String pictureUrl;
    private String baseCurrency;
    private String timezone;
    private BigDecimal monthlyLimit;

    public static UserProfileDto from(UserProfile user) {
        return new UserProfileDto(
                user.getId().toString(),
                user.getTelegramId(),
                user.getDisplayName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.isGoogleLinked(),
                user.getPictureUrl(),
                user.getBaseCurrency(),
                user.getTimezone(),
                user.getMonthlyLimit()
        );
    }
}
