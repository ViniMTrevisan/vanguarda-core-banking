-- accounts indexes
CREATE INDEX idx_accounts_owner_id ON accounts(owner_id);
CREATE INDEX idx_accounts_status   ON accounts(status);

-- transactions indexes
CREATE INDEX idx_transactions_source       ON transactions(source_account_id);
CREATE INDEX idx_transactions_target       ON transactions(target_account_id);
CREATE INDEX idx_transactions_processed_at ON transactions(processed_at);

-- ledger_entries indexes
CREATE INDEX idx_ledger_account_id     ON ledger_entries(account_id);
CREATE INDEX idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_created_at     ON ledger_entries(created_at);
