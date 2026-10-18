ALTER TABLE app.user_account
    DROP CONSTRAINT ck_user_account_role;

UPDATE app.user_account
SET role = 'ADMIN'
WHERE role = 'MANAGER';

ALTER TABLE app.user_account
    ADD CONSTRAINT ck_user_account_role
    CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'));
