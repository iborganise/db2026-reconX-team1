-- ============================================================================
-- TICKET-ADV007 — Convert trades to monthly range-partitioned table
-- ============================================================================

-- 1. Rename existing table
ALTER TABLE trades RENAME TO trades_legacy;


-- 2. Create partitioned parent table
CREATE TABLE trades (
    id              BIGSERIAL,
    trade_ref       VARCHAR(30)   NOT NULL,
    instrument_id   BIGINT        NOT NULL REFERENCES instruments(id),
    counterparty_id BIGINT        NOT NULL REFERENCES counterparties(id),
    asset_class     VARCHAR(20)   NOT NULL,
    side            VARCHAR(4)    NOT NULL,
    quantity        NUMERIC(18,4) NOT NULL,
    price           NUMERIC(18,4) NOT NULL,
    trade_date      DATE          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modified_at     TIMESTAMPTZ,

    PRIMARY KEY (id, trade_date)

) PARTITION BY RANGE (trade_date);



-- 3. Monthly partitions April - July 2026

CREATE TABLE trades_y2026m04 PARTITION OF trades
FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');


CREATE TABLE trades_y2026m05 PARTITION OF trades
FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');


CREATE TABLE trades_y2026m06 PARTITION OF trades
FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');


CREATE TABLE trades_y2026m07 PARTITION OF trades
FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');



-- 4. Safety catch for out-of-window inserts

CREATE TABLE trades_default PARTITION OF trades DEFAULT;



-- 5. Indexes (created on parent, inherited by partitions)

CREATE INDEX idx_trades_status
ON trades(status);

CREATE INDEX idx_trades_instrument
ON trades(instrument_id);

CREATE INDEX idx_trades_counterparty
ON trades(counterparty_id);



-- 6. Copy old data into partitions

INSERT INTO trades
SELECT *
FROM trades_legacy;



-- 7. Remove after verification
-- DROP TABLE trades_legacy;
