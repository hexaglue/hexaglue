package com.banking.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Account aggregate persistence.
 *
 * @since 5.0.0
 */
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId id);

    List<Account> findAll();

    void delete(Account account);
}
