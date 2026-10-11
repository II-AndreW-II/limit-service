-- Monthly limits (immutable: new rows only, never update)
CREATE TABLE monthly_limit (
    id              BIGSERIAL PRIMARY KEY,
    account_from    VARCHAR(10)  NOT NULL,
    expense_category VARCHAR(20) NOT NULL,
    limit_sum       NUMERIC(19, 2) NOT NULL,
    limit_datetime  TIMESTAMPTZ  NOT NULL,
    currency_shortname VARCHAR(3) NOT NULL DEFAULT 'USD',
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_limit_account_category
    ON monthly_limit (account_from, expense_category, limit_datetime);

-- Transactions
CREATE TABLE transaction (
    id                  BIGSERIAL PRIMARY KEY,
    account_from        VARCHAR(10)  NOT NULL,
    account_to          VARCHAR(10)  NOT NULL,
    currency_shortname  VARCHAR(3)   NOT NULL,
    sum                 NUMERIC(19, 2) NOT NULL,
    sum_usd             NUMERIC(19, 4) NOT NULL,
    expense_category    VARCHAR(20)  NOT NULL,
    datetime            TIMESTAMPTZ  NOT NULL,
    limit_exceeded      BOOLEAN      NOT NULL DEFAULT FALSE,
    limit_id            BIGINT       REFERENCES monthly_limit (id)
);

CREATE INDEX idx_tx_account_category_dt
    ON transaction (account_from, expense_category, datetime);

CREATE INDEX idx_tx_limit_exceeded
    ON transaction (account_from, limit_exceeded);

-- Exchange rates cache
CREATE TABLE exchange_rate (
    id              BIGSERIAL PRIMARY KEY,
    currency_pair   VARCHAR(10)  NOT NULL,
    rate_date       DATE         NOT NULL,
    close_rate      NUMERIC(19, 8),
    previous_close  NUMERIC(19, 8),
    source          VARCHAR(50),
    fetched_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_pair_date UNIQUE (currency_pair, rate_date)
);

CREATE INDEX idx_rate_pair_date ON exchange_rate (currency_pair, rate_date);
