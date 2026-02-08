package com.banking.domain;

import java.util.UUID;

/**
 * Account identifier.
 *
 * @since 5.0.0
 */
public record AccountId(UUID value) {

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
