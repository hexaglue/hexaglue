package com.banking.domain;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for account management use cases.
 *
 * @since 5.0.0
 */
public interface ManagingAccounts {

    Account openAccount(String ownerName, long initialBalance);

    void deposit(AccountId accountId, long amount);

    void withdraw(AccountId accountId, long amount);

    Optional<Account> findAccount(AccountId accountId);

    List<Account> listAllAccounts();
}
