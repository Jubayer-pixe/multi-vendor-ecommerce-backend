-- Widening the invoice snapshot name preserves existing values.
ALTER TABLE invoices ALTER COLUMN customer_name TYPE VARCHAR(255);
