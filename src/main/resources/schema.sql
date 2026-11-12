CREATE TABLE IF NOT EXISTS member (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255) NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS product (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL,
    status      VARCHAR(30)  NOT NULL COMMENT 'ON_SALE, STOPPED',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_product_price CHECK (price >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS stock (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    product_id  BIGINT      NOT NULL,
    on_hand     INT         NOT NULL COMMENT '보유 수량',
    reserved    INT         NOT NULL DEFAULT 0 COMMENT '결제 대기 주문이 예약한 수량',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_product (product_id),
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_stock_on_hand  CHECK (on_hand >= 0),
    CONSTRAINT ck_stock_reserved CHECK (reserved >= 0 AND reserved <= on_hand)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS coupon (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(100) NOT NULL,
    discount_type        VARCHAR(30)  NOT NULL COMMENT 'FIXED, PERCENT',
    discount_value       BIGINT       NOT NULL,
    min_order_amount     BIGINT       NOT NULL DEFAULT 0,
    max_discount_amount  BIGINT       NULL COMMENT 'PERCENT 쿠폰의 할인 상한',
    valid_from           DATETIME(6)  NOT NULL,
    valid_until          DATETIME(6)  NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_coupon_value  CHECK (discount_value > 0),
    CONSTRAINT ck_coupon_period CHECK (valid_from < valid_until)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS member_coupon (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    coupon_id   BIGINT      NOT NULL,
    status      VARCHAR(30) NOT NULL COMMENT 'AVAILABLE, USED',
    used_at     DATETIME(6) NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_coupon_member_coupon (member_id, coupon_id),
    KEY idx_member_coupon_coupon (coupon_id),
    CONSTRAINT fk_member_coupon_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS point_wallet (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    balance     BIGINT      NOT NULL DEFAULT 0,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_wallet_member (member_id),
    CONSTRAINT fk_point_wallet_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT ck_point_wallet_balance CHECK (balance >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    order_no          VARCHAR(64)  NOT NULL COMMENT '외부 노출용 주문번호. PG의 orderId로 사용',
    idempotency_key   VARCHAR(64)  NOT NULL COMMENT '클라이언트가 보낸 요청 멱등키',
    member_id         BIGINT       NOT NULL,
    member_coupon_id  BIGINT       NULL,
    status            VARCHAR(30)  NOT NULL COMMENT 'PENDING, PAID, FAILED, EXPIRED, CANCEL_REQUESTED, CANCELED',
    total_amount      BIGINT       NOT NULL,
    discount_amount   BIGINT       NOT NULL DEFAULT 0,
    point_amount      BIGINT       NOT NULL DEFAULT 0,
    pay_amount        BIGINT       NOT NULL,
    failure_reason    VARCHAR(255) NULL,
    ordered_at        DATETIME(6)  NOT NULL,
    expires_at        DATETIME(6)  NULL COMMENT '결제 기한. 결제가 시작되거나 주문이 끝나면 NULL',
    paid_at           DATETIME(6)  NULL,
    canceled_at       DATETIME(6)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    UNIQUE KEY uk_orders_member_idempotency (member_id, idempotency_key),
    KEY idx_orders_member (member_id),
    KEY idx_orders_expires (expires_at),
    KEY idx_orders_member_coupon (member_coupon_id),
    CONSTRAINT fk_orders_member        FOREIGN KEY (member_id)        REFERENCES member (id),
    CONSTRAINT fk_orders_member_coupon FOREIGN KEY (member_coupon_id) REFERENCES member_coupon (id),
    CONSTRAINT ck_orders_amount CHECK (pay_amount = total_amount - discount_amount - point_amount),
    CONSTRAINT ck_orders_pay    CHECK (pay_amount >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_item (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    order_id      BIGINT       NOT NULL,
    product_id    BIGINT       NOT NULL,
    product_name  VARCHAR(100) NOT NULL COMMENT '주문 시점 스냅샷',
    unit_price    BIGINT       NOT NULL COMMENT '주문 시점 스냅샷',
    quantity      INT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_order_product (order_id, product_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS stock_history (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    product_id      BIGINT      NOT NULL,
    order_id        BIGINT      NULL COMMENT 'INBOUND는 NULL',
    type            VARCHAR(30) NOT NULL COMMENT 'INBOUND, RESERVE, COMMIT, RELEASE, RESTOCK',
    quantity        INT         NOT NULL,
    on_hand_after   INT         NOT NULL,
    reserved_after  INT         NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_history_order_product_type (order_id, product_id, type),
    KEY idx_stock_history_product (product_id),
    CONSTRAINT fk_stock_history_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_stock_history_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT ck_stock_history_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS point_history (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    member_id      BIGINT      NOT NULL,
    order_id       BIGINT      NULL COMMENT 'CHARGE는 NULL',
    type           VARCHAR(30) NOT NULL COMMENT 'CHARGE, USE, RESTORE',
    amount         BIGINT      NOT NULL,
    balance_after  BIGINT      NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_history_order_type (order_id, type),
    KEY idx_point_history_member (member_id),
    CONSTRAINT fk_point_history_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_point_history_order  FOREIGN KEY (order_id)  REFERENCES orders (id),
    CONSTRAINT ck_point_history_amount CHECK (amount > 0)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS payment (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_id         BIGINT       NOT NULL,
    payment_key      VARCHAR(200) NULL COMMENT 'PG가 발급한 결제 키',
    method           VARCHAR(30)  NULL,
    amount           BIGINT       NOT NULL,
    status           VARCHAR(30)  NOT NULL COMMENT 'READY, APPROVED, FAILED, CANCELED',
    failure_code     VARCHAR(50)  NULL,
    failure_message  VARCHAR(255) NULL,
    approved_at      DATETIME(6)  NULL,
    canceled_at      DATETIME(6)  NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order (order_id),
    UNIQUE KEY uk_payment_payment_key (payment_key),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE = InnoDB;