package io.hexaglue.arch.integration.fixtures.domain;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Value object representing monetary amount.
 */
@ValueObject
public record Money(BigDecimal amount, Currency currency) {

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money euros(BigDecimal amount) {
        return of(amount, "EUR");
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
}
