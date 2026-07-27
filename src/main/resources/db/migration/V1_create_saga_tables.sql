CREATE TABLE saga_instance (
    id                      BINARY(16)   NOT NULL,
    saga_id                 BINARY(16)   NOT NULL,
    order_id                BINARY(16)   NOT NULL,
    status                  VARCHAR(30)  NOT NULL,
    current_state           VARCHAR(50)  NOT NULL,
    correlation_id          VARCHAR(100) NOT NULL,
    customer_id             BINARY(16),
    total_amount            DECIMAL(19,2),
    compensation_steps_done INT          NOT NULL DEFAULT 0,
    stock_compensated       BOOLEAN      NOT NULL DEFAULT FALSE,
    payment_compensated     BOOLEAN      NOT NULL DEFAULT FALSE,
    order_compensated       BOOLEAN      NOT NULL DEFAULT FALSE,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_saga_instance_saga_id (saga_id),
    INDEX idx_saga_instance_order_id (order_id),
    INDEX idx_saga_instance_status   (status),
    INDEX idx_saga_instance_updated  (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE saga_step (
    id            BINARY(16)   NOT NULL,
    saga_id       BINARY(16)   NOT NULL,
    step_name     VARCHAR(60)  NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    error_message TEXT,
    created_at    DATETIME(6)  NOT NULL,
    update_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_saga_step_saga_id (saga_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE saga_event (
    id             BINARY(16)   NOT NULL,
    saga_id        BINARY(16)   NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    payload        TEXT,
    correlation_id VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_saga_event_saga_id    (saga_id),
    INDEX idx_saga_event_event_type (event_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;