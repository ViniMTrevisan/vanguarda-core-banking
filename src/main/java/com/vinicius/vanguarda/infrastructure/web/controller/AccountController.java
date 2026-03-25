package com.vinicius.vanguarda.infrastructure.web.controller;

import com.vinicius.vanguarda.application.usecase.*;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import com.vinicius.vanguarda.infrastructure.web.dto.request.CreateAccountRequest;
import com.vinicius.vanguarda.infrastructure.web.dto.request.UpdateAccountStatusRequest;
import com.vinicius.vanguarda.infrastructure.web.dto.response.AccountResponse;
import com.vinicius.vanguarda.infrastructure.web.dto.response.BalanceResponse;
import com.vinicius.vanguarda.infrastructure.web.dto.response.LedgerEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
@Tag(name = "Accounts", description = "Account management operations")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final UpdateAccountStatusUseCase updateAccountStatusUseCase;
    private final GetAccountStatementUseCase getAccountStatementUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase,
                              GetAccountUseCase getAccountUseCase,
                              ListAccountsUseCase listAccountsUseCase,
                              UpdateAccountStatusUseCase updateAccountStatusUseCase,
                              GetAccountStatementUseCase getAccountStatementUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.listAccountsUseCase = listAccountsUseCase;
        this.updateAccountStatusUseCase = updateAccountStatusUseCase;
        this.getAccountStatementUseCase = getAccountStatementUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        CreateAccountUseCase.Input input = new CreateAccountUseCase.Input(
                request.ownerId(), request.ownerName(), request.currency(), request.initialBalance());
        CreateAccountUseCase.Output output = createAccountUseCase.execute(input);
        AccountResponse response = new AccountResponse(output.accountId(), output.ownerId(), output.ownerName(),
                output.currency(), output.balance(), output.status(), output.createdAt());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(output.accountId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID accountId) {
        GetAccountUseCase.Output output = getAccountUseCase.execute(accountId);
        AccountResponse response = new AccountResponse(output.accountId(), output.ownerId(), output.ownerName(),
                output.currency(), output.balance(), output.status(), output.createdAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List accounts")
    public ResponseEntity<Page<AccountResponse>> list(
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ListAccountsUseCase.Output> accounts = listAccountsUseCase.execute(ownerId, status, pageable);
        return ResponseEntity.ok(accounts.map(o -> new AccountResponse(
                o.accountId(), o.ownerId(), o.ownerName(), o.currency(), o.balance(), o.status(), o.createdAt())));
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId) {
        GetAccountUseCase.Output output = getAccountUseCase.execute(accountId);
        BalanceResponse response = new BalanceResponse(output.accountId(), output.balance(),
                output.currency(), Instant.now());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{accountId}/status")
    @Operation(summary = "Update account status (FROZEN or CLOSED)")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID accountId,
                                              @Valid @RequestBody UpdateAccountStatusRequest request) {
        updateAccountStatusUseCase.execute(accountId, request.status());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Get account statement (ledger entries)")
    public ResponseEntity<Page<LedgerEntryResponse>> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Page<GetAccountStatementUseCase.Output> entries = getAccountStatementUseCase
                .execute(accountId, type, fromInstant, toInstant, pageable);

        return ResponseEntity.ok(entries.map(e -> new LedgerEntryResponse(
                e.entryId(), e.transactionId(), e.accountId(), e.type(),
                e.amount(), e.balanceBefore(), e.balanceAfter(), e.createdAt())));
    }
}
