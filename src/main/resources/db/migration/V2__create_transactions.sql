CREATE TABLE transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key   VARCHAR(255) NOT NULL UNIQUE,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    target_account_id UUID NOT NULL REFERENCES accounts(id),
    amount            NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    description       VARCHAR(255),
    metadata          JSONB,
    processed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_different_accounts CHECK (source_account_id != target_account_id),
    CONSTRAINT chk_tx_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED', 'REVERSED'))
);
