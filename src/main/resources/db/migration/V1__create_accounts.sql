CREATE TABLE accounts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     VARCHAR(255) NOT NULL,
    owner_name   VARCHAR(100) NOT NULL,
    currency     VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    balance      NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_currency CHECK (currency IN ('BRL', 'USD', 'EUR'))
);
