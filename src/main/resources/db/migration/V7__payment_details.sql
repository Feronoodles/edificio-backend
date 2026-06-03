ALTER TABLE payments ADD COLUMN paid_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN payment_method VARCHAR(30);
ALTER TABLE payments ADD COLUMN reference VARCHAR(80);

UPDATE payments
SET paid_amount = amount
WHERE status = 'PAID';
