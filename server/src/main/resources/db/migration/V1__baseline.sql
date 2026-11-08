CREATE TABLE users (
  id                         UUID PRIMARY KEY,
  email                      TEXT NOT NULL,
  auth_hash                  BYTEA NOT NULL,   -- HMAC(pepper, authKey)
  recovery_auth_hash         BYTEA NOT NULL,   -- HMAC(pepper, recoveryAuthKey)
  kdf_salt                   BYTEA NOT NULL,   -- 16 байт
  kdf_params                 JSONB NOT NULL,   -- {"alg":"argon2id","m":65536,"t":3,"p":1}
  wrapped_vault_key          BYTEA NOT NULL,
  recovery_wrapped_vault_key BYTEA NOT NULL,
  sync_seq                   BIGINT NOT NULL DEFAULT 0,
  created_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE devices (
  id          UUID PRIMARY KEY,
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        TEXT NOT NULL,
  platform    TEXT NOT NULL CHECK (platform IN ('ios','android')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen   TIMESTAMPTZ,
  revoked_at  TIMESTAMPTZ
);

CREATE TABLE refresh_tokens (
  id          UUID PRIMARY KEY,
  device_id   UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  family_id   UUID NOT NULL,
  token_hash  BYTEA NOT NULL UNIQUE,           -- SHA-256 випадкового токена
  expires_at  TIMESTAMPTZ NOT NULL,
  family_created_at TIMESTAMPTZ NOT NULL,      -- абсолютний ліміт сім'ї (90 днів)
  used_at     TIMESTAMPTZ,
  revoked_at  TIMESTAMPTZ
);
CREATE INDEX idx_refresh_family ON refresh_tokens (family_id);

CREATE TABLE vault_items (
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  id          UUID NOT NULL,                   -- генерується клієнтом
  type        TEXT NOT NULL CHECK (type IN ('login','card','note','folder')),
  ciphertext  BYTEA,                           -- NULL для tombstone
  nonce       BYTEA,
  version     BIGINT NOT NULL,
  sync_seq    BIGINT NOT NULL,
  deleted_at  TIMESTAMPTZ,
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, id),
  CHECK (deleted_at IS NOT NULL OR (ciphertext IS NOT NULL AND nonce IS NOT NULL))
);
CREATE INDEX idx_items_sync ON vault_items (user_id, sync_seq);

CREATE TABLE idempotency_keys (
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  idempotency_key  UUID NOT NULL,
  body_hash        BYTEA NOT NULL,
  response         JSONB NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, idempotency_key)
);
