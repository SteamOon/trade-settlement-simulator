CREATE TABLE parties (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    party_id VARCHAR(64) NOT NULL,
    account_type VARCHAR(16) NOT NULL,
    currency CHAR(3),
    isin CHAR(12),
    balance NUMERIC(28, 8) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT accounts_type_instrument CHECK (
        (account_type = 'CASH' AND currency IS NOT NULL AND isin IS NULL)
        OR (account_type = 'SECURITY' AND isin IS NOT NULL AND currency IS NULL)
    )
);

CREATE UNIQUE INDEX accounts_party_cash_unique
    ON accounts (party_id, currency)
    WHERE account_type = 'CASH';

CREATE UNIQUE INDEX accounts_party_security_unique
    ON accounts (party_id, isin)
    WHERE account_type = 'SECURITY';

INSERT INTO parties (id, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'HOUSE'),
    ('22222222-2222-2222-2222-222222222222', 'COUNTERPARTY');

INSERT INTO accounts (id, party_id, account_type, currency, isin, balance) VALUES
    ('11111111-1111-1111-1111-111111111101', 'HOUSE-01', 'CASH', 'EUR', NULL, 1000000.00000000),
    ('11111111-1111-1111-1111-111111111102', 'HOUSE-01', 'SECURITY', NULL, 'US0378331005', 10000.00000000),
    ('22222222-2222-2222-2222-222222222201', 'COUNTERPARTY-01', 'CASH', 'EUR', NULL, 25000.00000000),
    ('22222222-2222-2222-2222-222222222202', 'COUNTERPARTY-01', 'SECURITY', NULL, 'US0378331005', 500.00000000);
