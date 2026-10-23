CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    key VARCHAR(255) NOT NULL,
    trade_id UUID REFERENCES trades (id),
    operation VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT idempotency_keys_trade_operation_key UNIQUE (trade_id, operation, key)
);
