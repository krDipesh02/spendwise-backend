package com.spendwise.config.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spendwise.telegram")
@Getter
@Setter
@NoArgsConstructor
public class TelegramProperties {
    private String botUsername = "";
    private int inviteExpirationMinutes = 30;
}
