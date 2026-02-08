package com.banking.domain;

/**
 * Notification gateway for account events.
 *
 * @since 5.0.0
 */
public interface AccountNotifier {

    void notifyDeposit(AccountId accountId, long amount);

    void notifyWithdrawal(AccountId accountId, long amount);
}
