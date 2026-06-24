-- Initial schema based on the GitHub Wiki ERD.
-- Redis-owned models such as search history v2, popular keywords, token state,
-- request deduplication, and distributed locks are intentionally excluded.

-- 1. MEMBER
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    token_invalid_before DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 2. CATEGORY
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(50) NOT NULL,
    depth INT NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id),
    CONSTRAINT uq_category_parent_name UNIQUE (parent_id, name)
);

-- 3. PRODUCT
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    sale_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES member(id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE INDEX idx_product_search ON product (sale_status, category_id, name);

-- 4. TIME_SALE
CREATE TABLE time_sale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    original_price DECIMAL(10, 2) NOT NULL,
    sale_price DECIMAL(10, 2) NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_time_sale_product FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 5. TIME_SALE_STOCK
CREATE TABLE time_sale_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_sale_id BIGINT NOT NULL UNIQUE,
    initial_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_time_sale_stock_sale FOREIGN KEY (time_sale_id) REFERENCES time_sale(id)
);

-- 6. TIME_SALE_ORDER
CREATE TABLE time_sale_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    time_sale_id BIGINT NOT NULL,
    request_id VARCHAR(36) NOT NULL UNIQUE,
    product_name_snapshot VARCHAR(100) NOT NULL,
    original_price_snapshot DECIMAL(10, 2) NOT NULL,
    sale_price_snapshot DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    ordered_at DATETIME NOT NULL,
    completed_member_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'COMPLETED' THEN member_id ELSE NULL END
    ) VIRTUAL,
    CONSTRAINT fk_order_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_order_time_sale FOREIGN KEY (time_sale_id) REFERENCES time_sale(id),
    CONSTRAINT uq_order_completed UNIQUE (completed_member_id, time_sale_id)
);

-- 7. COUPON_POLICY
CREATE TABLE coupon_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value BIGINT NOT NULL,
    max_discount_amount BIGINT NULL,
    issue_starts_at DATETIME NOT NULL,
    issue_ends_at DATETIME NOT NULL,
    total_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 8. ISSUED_COUPON
CREATE TABLE issued_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_policy_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    issued_at DATETIME NOT NULL,
    CONSTRAINT fk_coupon_policy FOREIGN KEY (coupon_policy_id) REFERENCES coupon_policy(id),
    CONSTRAINT fk_coupon_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT uq_issued_coupon_policy_member UNIQUE (coupon_policy_id, member_id)
);

-- 9. CHAT_ROOM
CREATE TABLE chat_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type VARCHAR(20) NOT NULL,
    seller_id BIGINT NULL,
    created_by_member_id BIGINT NOT NULL,
    cs_status VARCHAR(20) NULL,
    closed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_chat_room_seller FOREIGN KEY (seller_id) REFERENCES member(id),
    CONSTRAINT fk_chat_room_creator FOREIGN KEY (created_by_member_id) REFERENCES member(id),
    CONSTRAINT uq_chat_room_composite UNIQUE (room_type, seller_id, created_by_member_id)
);

-- 10. CHAT_ROOM_PARTICIPANT
CREATE TABLE chat_room_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    participant_role VARCHAR(20) NOT NULL,
    joined_at DATETIME NOT NULL,
    left_at DATETIME NULL,
    CONSTRAINT fk_participant_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    CONSTRAINT fk_participant_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT uq_participant_room_member UNIQUE (chat_room_id, member_id)
);

-- 11. CHAT_MESSAGE
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NULL,
    content VARCHAR(1000) NOT NULL,
    client_message_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    product_id BIGINT NULL,
    product_name_snapshot VARCHAR(100) NULL,
    product_price_snapshot DECIMAL(10, 2) NULL,
    sent_at DATETIME NOT NULL,
    CONSTRAINT fk_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES member(id),
    CONSTRAINT uq_message_composite UNIQUE (chat_room_id, sender_id, client_message_id)
);

-- 12. SEARCH_HISTORY
CREATE TABLE search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NULL,
    category_id BIGINT NULL,
    guest_uuid VARCHAR(50) NULL,
    keyword VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_search_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_search_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE INDEX idx_search_history_created_at ON search_history (created_at);
