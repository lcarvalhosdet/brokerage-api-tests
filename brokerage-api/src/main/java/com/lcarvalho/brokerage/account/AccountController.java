package com.lcarvalho.brokerage.account;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.lcarvalho.brokerage.order.OrderResponse;
import com.lcarvalho.brokerage.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final OrderService orderService;

    public AccountController(AccountService accountService, OrderService orderService) {
        this.accountService = accountService;
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(account.id())
                .toUri();
        return ResponseEntity.created(location).body(account);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return accountService.get(id);
    }

    @GetMapping("/{id}/orders")
    public List<OrderResponse> orders(@PathVariable UUID id) {
        return orderService.listByAccount(id);
    }
}
