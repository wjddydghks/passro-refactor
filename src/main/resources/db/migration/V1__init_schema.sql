CREATE TABLE place (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subway_route_name VARCHAR(255),
    subway_station_name VARCHAR(255),
    latitude DECIMAL(11, 8),
    longitude DECIMAL(11, 8),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mail VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    birth DATE NOT NULL,
    place_id_id BIGINT,
    point BIGINT,
    picture VARCHAR(255),
    role ENUM ('ADMIN', 'USER'),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_account_mail UNIQUE (mail),
    CONSTRAINT uk_account_nickname UNIQUE (nickname),
    CONSTRAINT uk_account_phone_number UNIQUE (phone_number),
    CONSTRAINT fk_account_place FOREIGN KEY (place_id_id) REFERENCES place (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE account_place (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT,
    start_place_id BIGINT,
    destination_place_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_account_place_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_account_place_start FOREIGN KEY (start_place_id) REFERENCES place (id),
    CONSTRAINT fk_account_place_destination FOREIGN KEY (destination_place_id) REFERENCES place (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE waypoint (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_place_id BIGINT,
    place_id BIGINT,
    visit_order INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_waypoint_account_place FOREIGN KEY (account_place_id) REFERENCES account_place (id),
    CONSTRAINT fk_waypoint_place FOREIGN KEY (place_id) REFERENCES place (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE university (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    mail_domain VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_university_name UNIQUE (name),
    CONSTRAINT uk_university_mail_domain UNIQUE (mail_domain)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    origin_id BIGINT,
    dest_id BIGINT,
    memo VARCHAR(255),
    status ENUM ('CANCEL', 'CONFIRM_REQUESTED', 'DELIVERED', 'DELIVERING', 'MATCHED', 'WAIT'),
    terms BIT,
    sender_id BIGINT,
    shipper_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_origin FOREIGN KEY (origin_id) REFERENCES place (id),
    CONSTRAINT fk_delivery_destination FOREIGN KEY (dest_id) REFERENCES place (id),
    CONSTRAINT fk_delivery_sender FOREIGN KEY (sender_id) REFERENCES account (id),
    CONSTRAINT fk_delivery_shipper FOREIGN KEY (shipper_id) REFERENCES account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery_good_info_seq (
    next_val BIGINT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO delivery_good_info_seq (next_val) VALUES (1);

CREATE TABLE delivery_info_seq (
    next_val BIGINT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO delivery_info_seq (next_val) VALUES (1);

CREATE TABLE delivery_point_seq (
    next_val BIGINT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO delivery_point_seq (next_val) VALUES (1);

CREATE TABLE delivery_good_info (
    id BIGINT NOT NULL,
    delivery_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price BIGINT,
    size VARCHAR(255),
    picture VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_good_info_delivery UNIQUE (delivery_id),
    CONSTRAINT fk_delivery_good_info_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery_info (
    id BIGINT NOT NULL,
    delivery_id BIGINT,
    picture VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_info_delivery UNIQUE (delivery_id),
    CONSTRAINT fk_delivery_info_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery_point (
    id BIGINT NOT NULL,
    delivery_id BIGINT NOT NULL,
    base_point BIGINT,
    distance_point BIGINT,
    weight_point BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_point_delivery UNIQUE (delivery_id),
    CONSTRAINT fk_delivery_point_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT,
    type ENUM ('CANCELED', 'DELIVERED', 'DONE', 'MATCHED', 'PICKED_UP', 'SEND_REQUEST') NOT NULL,
    image VARCHAR(500),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_log_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery_inquiry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT,
    account_id BIGINT,
    category ENUM ('DAMAGE', 'DELAY', 'ETC', 'LOST', 'POINT', 'WRONG_DELIVERY'),
    title VARCHAR(255),
    content TEXT,
    image_key VARCHAR(512),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_inquiry_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id),
    CONSTRAINT fk_delivery_inquiry_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE inquiry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    category ENUM ('ACCOUNT', 'BUG', 'DELIVERY', 'ETC', 'PAYMENT', 'SERVICE') NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    image_key VARCHAR(512),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiry_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE chat_room (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT NOT NULL,
    sender_left_at DATETIME(6),
    shipper_left_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_room_delivery UNIQUE (delivery_id),
    CONSTRAINT fk_chat_room_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    image_key VARCHAR(500),
    system_message BOOLEAN NOT NULL DEFAULT FALSE,
    is_read BIT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_message_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES account (id),
    INDEX idx_chat_message_delivery_id_id (delivery_id, id),
    INDEX idx_chat_message_unread (delivery_id, is_read, sender_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE market (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    image_key VARCHAR(255),
    category ENUM ('CAFE', 'CONVENIENCE_STORE', 'ETC', 'FOOD'),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE point_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    delivery_id BIGINT,
    market_id BIGINT,
    increment_reason ENUM ('DELIVERY_PAYMENT', 'DELIVERY_REFUND', 'DELIVERY_SETTLEMENT', 'MARKET_PURCHASE') NOT NULL,
    delta_point BIGINT NOT NULL,
    before_point BIGINT NOT NULL,
    after_point BIGINT NOT NULL,
    increment_reason_memo VARCHAR(500),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_point_log_account_delivery_reason UNIQUE (account_id, delivery_id, increment_reason),
    CONSTRAINT fk_point_log_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_point_log_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id),
    CONSTRAINT fk_point_log_market FOREIGN KEY (market_id) REFERENCES market (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    type ENUM ('DELIVERY', 'GENERAL') NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    resource_type ENUM ('DELIVERY', 'NONE') NOT NULL,
    resource_id BIGINT,
    is_read BIT NOT NULL,
    read_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    reported_account_id BIGINT,
    delivery_id BIGINT,
    chat_message_id BIGINT,
    target_type ENUM ('ACCOUNT', 'CHAT_MESSAGE', 'DELIVERY') NOT NULL,
    target_id BIGINT NOT NULL,
    reason ENUM ('ABUSE', 'FRAUD', 'HARASSMENT', 'INAPPROPRIATE_CONTENT', 'OTHER', 'SPAM') NOT NULL,
    detail VARCHAR(1000),
    status ENUM ('IN_REVIEW', 'PENDING', 'REJECTED', 'RESOLVED') NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_report_reporter_target UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES account (id),
    CONSTRAINT fk_report_reported_account FOREIGN KEY (reported_account_id) REFERENCES account (id),
    CONSTRAINT fk_report_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id),
    CONSTRAINT fk_report_chat_message FOREIGN KEY (chat_message_id) REFERENCES chat_message (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE report_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    image_key VARCHAR(255) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_report_image_report FOREIGN KEY (report_id) REFERENCES report (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT NOT NULL,
    rating INT,
    content VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_review_delivery UNIQUE (delivery_id),
    CONSTRAINT fk_review_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
