> Subsequent user-approved V5 standardization supersedes the temporary legacy-column
> coordination approach below. Live price/stock now come only from SKU/inventory; old values
> are immutable legacy_* archives. See `../contracts/Catalog_Shared_Contract.md` for current integration.

# HomeMart schema — decision and deployment evidence, 2026-10-05

User authorized finalizing the reviewed schema and applying it to the configured Supabase.
AnNP owns UC-04, UC-05, UC-13, UC-15, UC-32, UC-33 and UC-34. No business UC is claimed complete.

## Authority and compatibility

Decisions were recorded first in column I of the canonical 35-UC Backlog (rows 5, 6, 14,
15, 16, 20, 32, 33, 34 and 35). Top-level Lead cells for AnNP's seven UCs were synchronized;
iteration allocations still require the team's reconciliation. This document is an RDS
synchronization addendum, not proof that the live RDS or submitted revision was rewritten.

The target was not empty: V1–V3 were already applied, with three products and no accounts.
Exact V2/V3 sources were restored from GitLab commit
`5d0f1ad0598fe555739cfc2df86e618d27090e91`; applied migrations were not modified.
V2 checksum is -605359411; V3 checksum is -746559907.

Deploy only `backend/src/main/resources/db/migration/V4__create_homemart_business_foundation.sql`
through Flyway. `HomeMart_From_Diagram_PostgreSQL.sql` and its verification file are historical
review artifacts, not setup scripts for the shared database.

## Recorded contracts

- Keep `app.user_account`, BCrypt password hashes and CUSTOMER/STAFF/MANAGER. Domain FKs
  use this identity. RDS Admin naming still needs a consistent mapping to MANAGER.
- Reuse `app.product` from V2/V3. Add category/brand references and sale-unit `hm_sku`;
  require an internal store barcode, nonnegative whole-VND prices and a product image,
  category and brand before ACTIVE publication. INACTIVE permits incomplete catalogue data.
- Preserve legacy product values and create SKU/inventory/opening movement projections.
  Existing items lacking brand/image become INACTIVE. Migration timestamps describe the
  import; they do not invent historical stock transaction dates.
- Address defaults are unique among undeleted addresses. Service transactions must ensure
  a replacement default when addresses remain and enforce ownership.
- Payment attempts have stable idempotency references, provider/gateway identifiers and
  sanitized callback evidence. COMPLETED payments/refunds and history events are immutable.
  Late payment success needs reconciliation and must not reopen cancelled orders.
- Reserve once at checkout; order confirmation checks existing reservation. Service owners
  must implement locking, expiry and idempotent release; schema alone does not do this.
- UC-31 creates a return request; UC-32 records inspection/approval/restock evidence;
  UC-33 disburses one logical refund per request against the original order/payment.
  Partial returned items are allowed; multiple partial payout records are outside this baseline.
- Return lines must reference the purchased SKU on the same original order. Service checks
  still enforce cumulative returned quantities, approved status and remaining refund balance,
  including pending allocations. Staff inspection versus Manager approval needs RDS alignment.
- UC-34 is a read-only settled payment/refund report. Aggregate each flow separately and
  filter by its settlement time in Asia/Ho_Chi_Minh. It is not an accounting engine.

## Verification

Java 21 / Maven 3.9.16 / PostgreSQL 17: `mvn -B clean verify` passed 7 integration tests.
A separate local upgrade check used real Flyway to apply V1–V3, inserted a legacy product,
applied V4, and verified price/stock preservation, inactive publication, SKU/inventory and
one opening ledger entry. Repeating migrate executed zero migrations.

Supabase Flyway applied exactly one migration (V4), validated successfully, and a second
run executed zero. Current version 4, pending 0; all V1–V4 history entries successful.
There are 25 app tables including history, three existing products now INACTIVE, three SKUs,
three inventory rows (physical total 65, reserved 0), three opening movements, zero accounts,
zero parallel identity/product tables and zero price/stock projection mismatches.
The before/after aggregate digest matched. Backlog proof is saved under `evidence/`.

Read-only verification compares
an aggregate digest of original product columns before/after (excluding intentional status
changes and newly added columns); no production test fixtures or bootstrap accounts are added.

Cross-platform commands from repository root, with JDK 21 and local ignored DB environment:

```text
npm run build
npm run db:info
npm run db:migrate
npm run db:info
```

The explicit migration command runs Flyway without starting the web server, sending mail or
bootstrapping a Manager. It disables clean/baseline and suppresses connection details on errors.
Normal Spring startup continues to run Flyway and Hibernate validation.

## Remaining integration work

Implement AnNP's UCs one vertical slice at a time. Keep legacy `product.price` and
`product.stock_quantity` coordinated with SKU/inventory writes until teammates agree to retire
those projections; no automatic two-way synchronization is implied. Update product APIs/JPA
mappings for publication requirements before creating new ACTIVE products. Reconcile iteration
allocations, BR-38/39/40/41/42 collisions, Admin/MANAGER wording, RDS descriptions and ERD.
Select one payment provider before implementing UC-15; provider configuration is not included.
The shared stock/order/procurement tables are prerequisites, not implementations of other owners' UCs.
