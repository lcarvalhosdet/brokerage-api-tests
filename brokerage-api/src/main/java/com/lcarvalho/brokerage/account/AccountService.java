package com.lcarvalho.brokerage.account;

import java.util.UUID;

import com.lcarvalho.brokerage.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        UUID id = UUID.randomUUID();
        // Validation already guarantees at most two decimal places, so no rounding happens here
        accounts.insert(id, request.ownerName().trim(), request.initialCash().setScale(2));
        return get(id);
    }

    /**
     * Reads the account and its positions from one snapshot of the database. With the default
     * READ COMMITTED level, each query sees its own snapshot, so an order filled between the two
     * queries would show the cash from before the order next to the positions from after it.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AccountResponse get(UUID id) {
        AccountRepository.AccountRow account = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "Account " + id + " not found"));
        return new AccountResponse(account.id(), account.ownerName(), account.cashBalance(),
                accounts.findPositions(id), account.createdAt());
    }
}
