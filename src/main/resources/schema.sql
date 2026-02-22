-- Migrations for existing tables
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='is_verified') THEN
        ALTER TABLE users RENAME COLUMN is_verified TO verified;
    END IF;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS users (
    id SERIAL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    roles TEXT[],
    ip_address VARCHAR(45),
    city VARCHAR(100),
    country VARCHAR(100),
    device_data TEXT,
    profile_type VARCHAR(50),
    verified BOOLEAN DEFAULT FALSE,
    verification_code VARCHAR(255),
    verification_code_expires_at TIMESTAMP,
    last_login_at TIMESTAMP,
    last_active_at TIMESTAMP,
    last_session_duration BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (email),
    PRIMARY KEY (id, email)
) PARTITION BY HASH (email);

CREATE TABLE IF NOT EXISTS users_p0 PARTITION OF users FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE IF NOT EXISTS users_p1 PARTITION OF users FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE IF NOT EXISTS users_p2 PARTITION OF users FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE IF NOT EXISTS users_p3 PARTITION OF users FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
