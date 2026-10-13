-- BuchHaus schema (MySQL 8; also runs on H2 in MySQL mode). Money is stored in paise (BIGINT).

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(80)  NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE books (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku            VARCHAR(20)   NOT NULL,
    title          VARCHAR(160)  NOT NULL,
    author         VARCHAR(120)  NOT NULL,
    genre          VARCHAR(40)   NOT NULL,
    description    VARCHAR(2000),
    price_paise    BIGINT        NOT NULL,
    stock          INT           NOT NULL,
    published_year INT,
    pages          INT,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP(6)  NOT NULL,
    CONSTRAINT uk_books_sku UNIQUE (sku),
    CONSTRAINT ck_books_price CHECK (price_paise >= 0),
    CONSTRAINT ck_books_stock CHECK (stock >= 0)
);
CREATE INDEX idx_books_genre ON books (active, genre);
CREATE INDEX idx_books_title ON books (title);

CREATE TABLE cart_items (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    book_id  BIGINT NOT NULL,
    quantity INT    NOT NULL,
    CONSTRAINT uk_cart_user_book UNIQUE (user_id, book_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_quantity CHECK (quantity BETWEEN 1 AND 10)
);

CREATE TABLE orders (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    status         VARCHAR(12)  NOT NULL,
    subtotal_paise BIGINT       NOT NULL,
    shipping_paise BIGINT       NOT NULL,
    total_paise    BIGINT       NOT NULL,
    ship_name      VARCHAR(80)  NOT NULL,
    ship_phone     VARCHAR(15)  NOT NULL,
    ship_address   VARCHAR(200) NOT NULL,
    ship_city      VARCHAR(60)  NOT NULL,
    ship_pincode   VARCHAR(6)   NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL,
    updated_at     TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_orders_total CHECK (total_paise = subtotal_paise + shipping_paise)
);
CREATE INDEX idx_orders_user ON orders (user_id, created_at);
CREATE INDEX idx_orders_status ON orders (status, created_at);

CREATE TABLE order_items (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id         BIGINT       NOT NULL,
    book_id          BIGINT       NOT NULL,
    title_snapshot   VARCHAR(160) NOT NULL,
    unit_price_paise BIGINT       NOT NULL,
    quantity         INT          NOT NULL,
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT ck_items_quantity CHECK (quantity > 0)
);
