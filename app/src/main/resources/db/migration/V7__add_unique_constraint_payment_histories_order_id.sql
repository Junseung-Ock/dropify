ALTER TABLE payment_histories
    ADD CONSTRAINT uk_payment_histories_order_id UNIQUE (order_id);
