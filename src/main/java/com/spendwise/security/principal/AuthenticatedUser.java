package com.spendwise.security.principal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {

    private UUID userId;
    private AuthenticationType authenticationType;
}
