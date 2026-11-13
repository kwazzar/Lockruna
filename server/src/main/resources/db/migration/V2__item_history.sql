CREATE TABLE item_history (
  user_id     UUID NOT NULL,
  item_id     UUID NOT NULL,
  type        TEXT NOT NULL CHECK (type IN ('login','card','note','folder')),
  version     BIGINT NOT NULL,
  ciphertext  BYTEA NOT NULL,
  nonce       BYTEA NOT NULL,
  updated_at  TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (user_id, item_id, version),
  FOREIGN KEY (user_id, item_id)
    REFERENCES vault_items(user_id, id) ON DELETE CASCADE
);

CREATE INDEX idx_item_history_item
  ON item_history (user_id, item_id, version DESC);