package com.banking.domain;

import java.util.UUID;

/**
 * Transaction identifier.
 *
 * @since 5.0.0
 */
public record TransactionId(UUID value) {

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }
}
