CREATE TABLE payment_histories
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    order_id   BIGINT      NOT NULL,
    amount     BIGINT      NOT NULL,
    paid_at    DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_histories_user_id  FOREIGN KEY (user_id)  REFERENCES users (id),
    CONSTRAINT fk_payment_histories_order_id FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_payment_histories_user_id        ON payment_histories (user_id);
CREATE INDEX idx_payment_histories_user_id_paid_at ON payment_histories (user_id, paid_at);
