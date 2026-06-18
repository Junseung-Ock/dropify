-- ──────────────────────────────────────────────────────────────────────────────
-- stock_histories
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE stock_histories
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    product_id      BIGINT       NOT NULL,
    change_type     VARCHAR(20)  NOT NULL COMMENT 'DECREASE | REPLENISH | SYNC',
    before_quantity INT          NOT NULL,
    after_quantity  INT          NOT NULL,
    change_quantity INT          NOT NULL,
    changed_by      VARCHAR(100) NOT NULL COMMENT 'userId or SYSTEM',
    reason          VARCHAR(255),
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_stock_histories_change_type CHECK (change_type IN ('DECREASE', 'REPLENISH', 'SYNC')),
    CONSTRAINT fk_stock_histories_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_stock_histories_product_id  ON stock_histories (product_id);
CREATE INDEX idx_stock_histories_created_at  ON stock_histories (created_at);
