-- Clutch cloud backup & sync schema (MySQL 5.7+ / MariaDB 10.3+)
-- Every farm row is owned by one user and keyed by the same id the phone uses,
-- so a phone can push and pull without id mapping.
-- updated_at        = when the farmer changed the row (phone clock, ms) — used for last-write-wins
-- server_updated_at = when the server stored it (server clock, ms)       — used as the pull cursor

CREATE DATABASE IF NOT EXISTS clutch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clutch;

CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS auth_tokens (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    device_name VARCHAR(80) NOT NULL DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at DATETIME NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS login_attempts (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_attempts_phone_time (phone, attempted_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS flocks (
    user_id INT UNSIGNED NOT NULL,
    id VARCHAR(64) NOT NULL,
    name VARCHAR(80) NOT NULL,
    breed VARCHAR(80) NOT NULL,
    hens_housed INT NOT NULL,
    placed_date CHAR(10) NOT NULL,
    age_weeks_at_placement INT NOT NULL,
    archived TINYINT(1) NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    server_updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, id),
    INDEX idx_flocks_cursor (user_id, server_updated_at),
    CONSTRAINT fk_flocks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS daily_records (
    user_id INT UNSIGNED NOT NULL,
    id VARCHAR(100) NOT NULL,
    flock_id VARCHAR(64) NOT NULL,
    date CHAR(10) NOT NULL,
    live_hens INT NOT NULL,
    eggs INT NOT NULL,
    cracked INT NOT NULL DEFAULT 0,
    dirty INT NOT NULL DEFAULT 0,
    soft_shell INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    culls INT NOT NULL DEFAULT 0,
    feed_kg DOUBLE NOT NULL DEFAULT 0,
    water_l DOUBLE NOT NULL DEFAULT 0,
    avg_egg_weight_g DOUBLE NULL,
    max_temp_c DOUBLE NULL,
    body_weight_g DOUBLE NULL,
    uniformity_pct DOUBLE NULL,
    notes TEXT NOT NULL,
    entered_by VARCHAR(80) NOT NULL DEFAULT '',
    entered_by_name VARCHAR(80) NOT NULL DEFAULT '',
    updated_at BIGINT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    server_updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, id),
    INDEX idx_records_cursor (user_id, server_updated_at),
    CONSTRAINT fk_records_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS weekly_notes (
    user_id INT UNSIGNED NOT NULL,
    id VARCHAR(100) NOT NULL,
    flock_id VARCHAR(64) NOT NULL,
    age_week INT NOT NULL,
    notes TEXT NOT NULL,
    actions TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    server_updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, id),
    INDEX idx_notes_cursor (user_id, server_updated_at),
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
