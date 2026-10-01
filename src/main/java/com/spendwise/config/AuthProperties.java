package com.spendwise.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spendwise.auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthProperties {

    private String googleSuccessRedirectUrl;
}
