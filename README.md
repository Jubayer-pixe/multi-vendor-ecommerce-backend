# Spring Boot role based security API

See [API.md](API.md) for the complete endpoint reference, role matrix, DTO shapes, and Angular 17 integration examples.

This API uses stateless JWT authentication and a fixed role set stored as enum values. New accounts created through `/api/auth/signup` receive `ROLE_USER`; only an administrator can assign or change roles.

## Configuration

Set these environment variables before starting the application:

- `JWT_SECRET`: Base64 encoded random secret of at least 32 bytes. Use a new secret in each environment and rotate any secret previously committed to source control.
- `DB_URL`, `DB_USER`, `DB_PASS`: PostgreSQL connection settings. Local defaults are provided for the URL and username; set the password for your local database.
- `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`: Optional first administrator credentials. Set all three values; the password must be at least 12 characters. The account is created only if its username does not already exist.
- `CORS_ALLOWED_ORIGINS`: Comma separated allowed origin patterns. Defaults to `http://localhost:4200`.

For local development, Spring loads values from the ignored project-root `.env` file. A random `JWT_SECRET` has been generated there for this checkout; set `DB_PASS` in that file if your local PostgreSQL requires a password. Keep `.env` out of source control.

### Invoice database setup

This repository does not include Flyway or Liquibase. Apply the manual PostgreSQL scripts in order through the normal DBA migration process: `V20261104_01__invoice_tables.sql`, `V20261104_02__checkout_idempotency.sql`, then `V20261104_03__invoice_customer_name_255.sql`. Do not apply them automatically from the application. The idempotency table stores each customer's key, request hash, and original JSON checkout response (including checkout response contact/address fields); apply the same access and retention controls used for invoice/customer data. The `(customer_id, idempotency_key)` unique constraint and a transaction-scoped lock on the authenticated user serialize retries. Retain idempotency rows for as long as keys must remain replay-safe. The name migration only widens `customer_name` from 200 to 255 characters and preserves stored data. These scripts do not create invoices for existing orders; historical invoice backfill remains a separate decision. The configured Hibernate `ddl-auto=update` remains enabled by the existing application configuration, so production deployment must control schema permissions/configuration and apply the reviewed SQL rather than relying on startup DDL.

Successful customer checkout requires an `Idempotency-Key` UUID header and saves the order, COD payment, invoice, and replay response in the same transaction. Reuse the same key for the same checkout attempt; the authenticated customer's key is scoped to that customer. Reusing a key with a materially different request returns `409`. Angular keeps the key in tab-scoped `sessionStorage` across retries/reloads, clears it on success or explicit abandonment, and preserves it if checkout data is edited so the backend can reject a changed retry. COD invoice payment status is persisted as `PENDING`.

The available roles are defined in `ERole`; the app does not create role rows or allow runtime role creation. On startup, existing assignments from the old `userrole` table are copied to `user_roles`; the old table is left untouched. Custom database roles that are not in `ERole` are skipped. It does not create a default admin account or include a default JWT signing secret. Docker files and the Docker based Render manifest have been removed; run the application with the Maven wrapper or your IDE.

## Endpoints

- `POST /api/auth/signup` and `POST /api/auth/signin` are public.
- `GET /api/products` is public. Reading a product by ID requires authentication; creating or updating products requires `ROLE_MODERATOR` or `ROLE_ADMIN`; deleting requires `ROLE_ADMIN`.
- `/api/admin/**` requires `ROLE_ADMIN`.
- `GET /api/admin/roles` lists the fixed enum roles. `PUT /api/admin/users/{username}/roles` assigns one or more roles using their enum names.
- The products API stores data in PostgreSQL. It seeds three examples on an empty database. `GET /api/products` is public; `GET /api/products/premium` requires `ROLE_PREMIUM_USER`; create/update requires `ROLE_MODERATOR` or `ROLE_ADMIN`; delete requires `ROLE_ADMIN`.
- Dashboards are API endpoints, not frontend pages: `/api/dashboard/user` is available to all signed-in roles; `/moderator`, `/admin`, and `/premium` require `ROLE_MODERATOR`, `ROLE_ADMIN`, and `ROLE_PREMIUM_USER`, respectively. Each returns role-appropriate metrics and a limited product list.

Example product create request (sign in as a moderator or admin and send the JWT as a bearer token):

```json
{
  "name": "Wireless Mouse",
  "category": "Accessories",
  "price": 24.99,
  "description": "Wireless optical mouse",
  "stock": 25,
  "active": true,
  "premium": false,
  "imageUrl": "https://example.com/mouse.jpg"
}
```

To promote a test account to moderator, an administrator can send `PUT /api/admin/users/{username}/roles` with `{"roles":["ROLE_MODERATOR"]}`. The admin can also use `ROLE_PREMIUM_USER` to test the premium catalog.
- `/api/users/**` requires authentication. Users can read or update their own profile; administrator only actions are annotated in the controller.
- All other endpoints require authentication, with their role checks applied by controller method security.

Send the login token on protected requests as `Authorization: Bearer <token>`.
