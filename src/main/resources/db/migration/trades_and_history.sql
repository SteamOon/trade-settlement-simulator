CREATE TABLE trades (
    id UUID PRIMARY KEY,
    side VARCHAR(8) NOT NULL,
    isin CHAR(12) NOT NULL,
    quantity NUMERIC(28, 8) NOT NULL,
    price_amount NUMERIC(28, 8) NOT NULL,
    price_currency CHAR(3) NOT NULL,
    our_party_id UUID NOT NULL REFERENCES parties (id),
    counterparty_id UUID NOT NULL REFERENCES parties (id),
    trade_date DATE NOT NULL,
    settlement_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT trades_side_check CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT trades_status_check CHECK (
        status IN ('PENDING', 'MATCHED', 'UNMATCHED', 'SETTLED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT trades_quantity_positive CHECK (quantity > 0),
    CONSTRAINT trades_price_non_negative CHECK (price_amount >= 0)
);

CREATE INDEX trades_due_settlement_idx
    ON trades (status, settlement_date);

CREATE TABLE trade_status_history (
    id UUID PRIMARY KEY,
    trade_id UUID NOT NULL REFERENCES trades (id),
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(512),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT trade_status_history_from_check CHECK (
        from_status IS NULL
        OR from_status IN ('PENDING', 'MATCHED', 'UNMATCHED', 'SETTLED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT trade_status_history_to_check CHECK (
        to_status IN ('PENDING', 'MATCHED', 'UNMATCHED', 'SETTLED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX trade_status_history_trade_idx
    ON trade_status_history (trade_id, occurred_at);
