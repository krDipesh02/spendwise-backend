package com.spendwise.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spendwise.jwt")
@Setter
@Getter
public class JwtProperties {
    private String issuer = "spendwise-backend";
    private String audience = "spendwise-frontend";
    private String privateKey = "";
    private String publicKey = "";
    private long accessTokenMinutes = 10;
    private long refreshTokenDays = 30;
    private boolean refreshCookieSecure = false;
    private String refreshCookieSameSite = "Lax";
    private String refreshCookiePath = "/api/v1/auth";
}
