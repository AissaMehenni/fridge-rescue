-- Schéma initial du frigo. SQL portable H2 / PostgreSQL.
CREATE TABLE food_item (
    id          UUID                        NOT NULL PRIMARY KEY,
    name        VARCHAR(80)                 NOT NULL,
    quantity    NUMERIC(10, 2)              NOT NULL CHECK (quantity > 0),
    unit        VARCHAR(16)                 NOT NULL,
    category    VARCHAR(16)                 NOT NULL,
    expires_on  DATE                        NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

-- Requête principale : tri par date de péremption.
CREATE INDEX idx_food_item_expires_on ON food_item (expires_on);
