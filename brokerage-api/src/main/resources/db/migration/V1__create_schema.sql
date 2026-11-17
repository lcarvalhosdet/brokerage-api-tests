CREATE TABLE instruments (
    symbol  VARCHAR(10)    PRIMARY KEY,
    name    VARCHAR(100)   NOT NULL,
    price   NUMERIC(19, 2) NOT NULL CHECK (price > 0)
);

CREATE TABLE accounts (
    id            UUID           PRIMARY KEY,
    owner_name    VARCHAR(100)   NOT NULL,
    -- The database also refuses a negative balance, as a last line of defense
    cash_balance  NUMERIC(19, 2) NOT NULL CHECK (cash_balance >= 0),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE positions (
    account_id  UUID        NOT NULL REFERENCES accounts (id),
    symbol      VARCHAR(10) NOT NULL REFERENCES instruments (symbol),
    quantity    BIGINT      NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (account_id, symbol)
);

CREATE TABLE orders (
    id          UUID           PRIMARY KEY,
    account_id  UUID           NOT NULL REFERENCES accounts (id),
    symbol      VARCHAR(10)    NOT NULL REFERENCES instruments (symbol),
    side        VARCHAR(4)     NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    price       NUMERIC(19, 2) NOT NULL,
    total       NUMERIC(19, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX orders_account_created_idx ON orders (account_id, created_at);
