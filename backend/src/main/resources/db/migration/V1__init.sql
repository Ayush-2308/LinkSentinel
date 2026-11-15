CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE links (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    url TEXT NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    screen_safe BOOLEAN NOT NULL,
    screen_reason VARCHAR(500) NOT NULL,
    screen_title VARCHAR(200) NOT NULL,
    screen_summary VARCHAR(1000) NOT NULL,
    screen_source VARCHAR(16) NOT NULL
);

CREATE INDEX links_owner_id_idx ON links (owner_id);

CREATE SEQUENCE link_code_seq START 1;

CREATE TABLE clicks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL,
    referrer VARCHAR(500),
    device_type VARCHAR(16) NOT NULL
);

CREATE INDEX clicks_link_id_idx ON clicks (link_id);
