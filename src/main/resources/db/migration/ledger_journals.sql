CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    trade_id UUID NOT NULL UNIQUE REFERENCES trades (id),
    booked_at TIMESTAMPTZ NOT NULL,
    description VARCHAR(512)
);

CREATE TABLE postings (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries (id),
    account_id UUID NOT NULL REFERENCES accounts (id),
    debit NUMERIC(28, 8) NOT NULL DEFAULT 0,
    credit NUMERIC(28, 8) NOT NULL DEFAULT 0,
    CONSTRAINT postings_debit_or_credit CHECK (
        (debit > 0 AND credit = 0)
        OR (credit > 0 AND debit = 0)
    )
);

CREATE INDEX postings_journal_idx ON postings (journal_entry_id);
CREATE INDEX postings_account_idx ON postings (account_id);
