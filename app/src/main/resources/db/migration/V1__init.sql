-- ──────────────────────────────────────────────────────────────────────────────
-- users
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE users
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
);

-- ──────────────────────────────────────────────────────────────────────────────
-- products
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE products
(
    id             BIGINT                                    NOT NULL AUTO_INCREMENT,
    name           VARCHAR(255)                              NOT NULL,
    description    TEXT,
    price          BIGINT                                    NOT NULL,
    stock_quantity INT                                       NOT NULL DEFAULT 0,
    status         ENUM ('ACTIVE', 'INACTIVE', 'SOLD_OUT')  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)                               NOT NULL,
    updated_at     DATETIME(6)                               NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_products_price          CHECK (price >= 0),
    CONSTRAINT chk_products_stock_quantity CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_products_status ON products (status);

-- ──────────────────────────────────────────────────────────────────────────────
-- orders
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE orders
(
    id           BIGINT                                              NOT NULL AUTO_INCREMENT,
    user_id      BIGINT                                             NOT NULL,
    status       ENUM ('PENDING', 'PAID', 'FAILED', 'CANCELLED')   NOT NULL DEFAULT 'PENDING',
    total_amount BIGINT                                             NOT NULL,
    created_at   DATETIME(6)                                        NOT NULL,
    updated_at   DATETIME(6)                                        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_orders_user_id   ON orders (user_id);
CREATE INDEX idx_orders_status    ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- ──────────────────────────────────────────────────────────────────────────────
-- order_items
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE order_items
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    order_id   BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL,
    unit_price BIGINT      NOT NULL,  -- 주문 시점 가격 스냅샷
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order_id   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_order_items_product_id FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity  CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id   ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- payments
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE payments
(
    id                BIGINT                                              NOT NULL AUTO_INCREMENT,
    order_id          BIGINT                                             NOT NULL,
    toss_payment_key  VARCHAR(200),
    status            ENUM ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    amount            BIGINT                                             NOT NULL,
    paid_at           DATETIME(6),
    created_at        DATETIME(6)                                        NOT NULL,
    updated_at        DATETIME(6)                                        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payments_order_id         (order_id),
    UNIQUE KEY uq_payments_toss_payment_key (toss_payment_key),
    CONSTRAINT fk_payments_order_id FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- ──────────────────────────────────────────────────────────────────────────────
-- notifications
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE notifications
(
    id         BIGINT                          NOT NULL AUTO_INCREMENT,
    user_id    BIGINT                          NOT NULL,
    type       ENUM ('EMAIL', 'PUSH', 'SMS')  NOT NULL,
    title      VARCHAR(255),
    message    TEXT                            NOT NULL,
    is_read    TINYINT(1)                      NOT NULL DEFAULT 0,
    created_at DATETIME(6)                     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_user_id         ON notifications (user_id);
CREATE INDEX idx_notifications_user_id_is_read ON notifications (user_id, is_read);

-- ──────────────────────────────────────────────────────────────────────────────
-- seed data
-- ──────────────────────────────────────────────────────────────────────────────
INSERT INTO users (email, password, name, phone, created_at, updated_at)
VALUES ('user1@dropify.com', '$2a$10$placeholder.hashed.password.one', '김철수', '010-1234-5678', NOW(6), NOW(6)),
       ('user2@dropify.com', '$2a$10$placeholder.hashed.password.two', '이영희', '010-9876-5432', NOW(6), NOW(6)),
       ('user3@dropify.com', '$2a$10$placeholder.hashed.password.thr', '박지성', '010-5555-1234', NOW(6), NOW(6));

INSERT INTO products (name, description, price, stock_quantity, status, created_at, updated_at)
VALUES ('Nike 에어맥스 90', '클래식 러닝화', 129000, 100, 'ACTIVE', NOW(6), NOW(6)),
       ('아디다스 울트라부스트', '고성능 쿠셔닝 운동화', 189000, 50,  'ACTIVE', NOW(6), NOW(6)),
       ('뉴발란스 990v5', '메이드 인 USA 프리미엄', 259000, 30,  'ACTIVE', NOW(6), NOW(6)),
       ('컨버스 척테일러', '올스타 캔버스 스니커즈', 79000,  200, 'ACTIVE', NOW(6), NOW(6)),
       ('한정판 콜라보 스니커즈', '수량 한정 드롭 아이템', 350000, 10,  'ACTIVE', NOW(6), NOW(6));
