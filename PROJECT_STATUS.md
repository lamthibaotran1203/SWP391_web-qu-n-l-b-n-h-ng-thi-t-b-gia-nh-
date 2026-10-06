# Project status

Updated 2026-10-06. Read before planning documents.

## Authority and ownership

- Canonical [35-UC Backlog](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit)
  governs ownership/scope. Live RDS is work in progress; historical 37-UC pack is unapproved.
- User-confirmed AnNP UCs: **04, 05, 13, 15, 32, 33, 34**. Iteration allocations still conflict.
- Backlog I6 records UC05 policy/session decisions; I14/J14 record UC13 schema/contracts and
  2026-10-06 BR14 management/API warning. RDS updates deferred by user; notes stay local.

## Release gate and evidence

- R1 skeleton locally verified earlier. Shared Supabase verified 2026-10-06: PostgreSQL17.6,
  Flyway V1–V7 successful, pending0, 28 app tables including history. No schema change in this task.
- Foundation MR !6 merged. Local An fast-forwarded to origin/develop 2a96d72 before this work.
  Current UC05/UC13 changes are uncommitted/unpushed. Unrelated ok.md deletion preserved.
- UC05 code/tests: own-account password change, shared policy, transactional BCrypt update,
  logout current/old sessions; PostgreSQL concurrency/rollback tests and packaged two-session HTTP smoke passed.
  Browser login/form/route refresh checked; complete manual password-change browser demo pending.
- UC13 backend implemented: atomic master+manual SKUs, management read/write, publication/identifier/
  whole-VND guards, parent/SKU versions, deactivation preserving history, dynamic lowStock.
  No migration/new table/inventory write. Plain Bootstrap management UI at /manager/catalog.
  [Management API and traceability](docs/contracts/UC13_Catalog_Management_Contract.md).
- 2026-10-06: **34 backend PostgreSQL tests (13 UC13), 13 frontend tests (6 UC13), lint/build/package passed**.
  UC13 covers permission/CSRF, duplicates, publication, stale/concurrent edits/creates, DB rollback,
  retained deleted references, demotion revocation and unchanged stock/archive/order snapshots.
- UC13 real browser: Manager login, create product+2SKUs, warning and duplicate barcode verified.
  Packaged HTTP smoke: price update, addSKU, stale409, deactivate preserving3SKUs/public404, legacy410 passed.
  Browser app previously stalled on native confirmation; the inline checkbox now passed a real browser
  test: unchecked save blocked, checked save deactivated while retaining SKU/quantity data.
  No full UC sign-off or hosted CI claim yet.
- 2026-10-06 plain UI: UC05/UC13/login use scoped white/square/no-animation styles. Frontend 13 tests,
  lint/build and Java21 package passed. Browser checked local login, direct UC05 URL+refresh,
  UC13 price/threshold edit and deactivation; desktop/mobile inspected with no page overflow.
  Teammate public search/view pages were not changed. Fetch confirms origin/develop still 2a96d72,
  already included by local An; no extra merge needed.
- Later fetch found origin/develop 50e8246 (registration/email verification with shared auth changes).
  This revision is not merged into the dirty UC05/UC13 branch; shared security/account/API changes
  need deliberate integration, not a blind pull. Current local tests use the 2a96d72 baseline plus this work.
- UI wording/photo update: management title/filters/actions, product and SKU forms, stock warnings and
  password errors use user-facing Vietnamese. Same state/transaction/byte-limit rules retained.
  Real SUNHOUSE photos added to three explicit demo fixtures (illustrative price/stock, opening ledgers).
  Seed ran twice without duplicates or resetting earlier edits. 13 frontend/34 PostgreSQL tests passed;
  desktop manager photo loads, “Sắp hết hàng”/“Hết hàng” and 390px mobile with no page overflow confirmed. Public search/view logic unchanged;
  shared CSS colours flattened. Public browser navigation was blocked by client, so no public visual smoke claim.
- Supabase demo now runs at http://localhost:8086 in homemart-supabase-demo using the configured
  JDBC database. Three SUNHOUSE fixtures with photos seeded transactionally, quantities8/2/0;
  existing products, accounts and stock edits preserved. A dedicated demo Manager was added via
  existing bootstrap; credentials are only in ignored docs/database/Supabase_Demo_Login_Local.md.
  HTTP login/me, read all three products and CSRF logout passed; browser verification recorded locally.
- Latest origin/develop is 91c1d69 (registration plus sales-order changes), not merged into local An.
  Shared Supabase already has teammates' V6/V7; local migrations remain V1–V5. Hibernate validation
  and packaged app startup succeeded against this shared database without migration edits.
- Previous homemart-plain-ui-app is stopped. Its isolated homemart-plain-ui-db and data remain local.
  Personal learning guides, RDS notes, login credentials and screenshots are ignored under docs/database/.

## Current/next and blockers

- Current: UC05/UC13 direct-link demo running against shared Supabase on localhost:8086; seed uploaded.
- Next: user completes manual UC05 password-change browser demo, then team review/CI and RDS/SDS
  synchronization, and integrates develop 91c1d69 shared auth/order changes. Other assigned UCs are not claimed implemented.
- Teammates must use management productId + skuId separately; old /products writes return410 for Manager,
  Customer/Staff denied. Public GET compatibility remains. Old legacy CRUD model removed.
- Open: Admin/MANAGER wording, BR collisions, payment provider and iteration ownership; registration/reset
  owners need shared PasswordPolicy. README still references removed .env.example / ARCHITECTURE.md.
  Dated shared read contract sort/filter wording needs reconciliation with teammate changes on develop.

## Exact checks

From repository root, Node24/npm11 and Docker running:

```text
npm run frontend:check
npm run backend:verify:postgres
```

With JDK21, `npm run build` packages frontend+backend. Never run integration fixtures against Supabase.
For configured database only, `npm run db:info`, `npm run db:migrate`, `npm run db:seed:catalog`.
Local migration files end at V5; shared Supabase is V7, pending0. No new UC13 migration required.
Supabase demo restart: `docker restart homemart-supabase-demo` (existing configured container).
Existing older catalogue preview localhost:8085 is not team deployment; its container is
homemart-catalog-preview and was not changed by this task.
The stopped local preview database is retained; removing it would discard its separate demo edits.
