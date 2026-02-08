package com.banking.domain;

import java.util.List;
import java.util.Optional;

/**
 * Application service orchestrating account use cases.
 *
 * @since 5.0.0
 */
public class AccountService implements ManagingAccounts {

    private final AccountRepository accountRepository;
    private final AccountNotifier accountNotifier;

    public AccountService(AccountRepository accountRepository, AccountNotifier accountNotifier) {
        this.accountRepository = accountRepository;
        this.accountNotifier = accountNotifier;
    }

    @Override
    public Account openAccount(String ownerName, long initialBalance) {
        Account account = new Account(AccountId.generate(), ownerName, initialBalance);
        return accountRepository.save(account);
    }

    @Override
    public void deposit(AccountId accountId, long amount) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.deposit(amount);
        accountRepository.save(account);
        accountNotifier.notifyDeposit(accountId, amount);
    }

    @Override
    public void withdraw(AccountId accountId, long amount) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.withdraw(amount);
        accountRepository.save(account);
        accountNotifier.notifyWithdrawal(accountId, amount);
    }

    @Override
    public Optional<Account> findAccount(AccountId accountId) {
        return accountRepository.findById(accountId);
    }

    @Override
    public List<Account> listAllAccounts() {
        return accountRepository.findAll();
    }
}
