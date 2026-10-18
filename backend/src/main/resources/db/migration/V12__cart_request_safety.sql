-- UC10: optimistic revision and durable receipts for safely retrying Add to Cart.
ALTER TABLE app.hm_cart ADD COLUMN version uuid NOT NULL DEFAULT gen_random_uuid();

CREATE TABLE app.hm_cart_add_request (
    user_id bigint NOT NULL REFERENCES app.user_account(user_id),
    request_id uuid NOT NULL,
    sku_id bigint NOT NULL,
    quantity integer NOT NULL CHECK (quantity > 0),
    created_at timestamptz NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY (user_id, request_id)
);
COMMENT ON TABLE app.hm_cart_add_request IS
    'Accepted add requests. Retain receipts so retries cannot add the quantity twice.';
