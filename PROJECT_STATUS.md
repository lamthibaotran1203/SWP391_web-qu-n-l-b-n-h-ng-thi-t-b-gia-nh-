# Project status

Updated 2026-10-07. Read this before using planning documents.

## Authority and ownership

- Canonical [35-UC Backlog](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit) governs IDs/ownership/scope.
  Live RDS is work in progress; the historical 37-UC pack is unapproved comparison material.
- User-confirmed AnNP UCs: **04, 05, 13, 15, 32, 33, 34**. Backlog Lead cells updated;
  iteration allocations still need reconciliation. No business UC is claimed complete.
- Current user supplied the canonical Backlog screenshot assigning Lý Phong Nhã **UC-08,
  UC-09**. This task implements UC-09, not AnNP's historical assignments or UC-10.
- Shared catalogue decisions recorded in Backlog I14/J14. RDS updates deferred by the user;
  local notes, draft diagram SQL and screenshots are ignored, not fresh-clone dependencies.

## Release gate and completed evidence

- R1 walking skeleton was locally verified earlier. Shared Supabase uses Flyway V1–V5;
  zero pending migrations, 24 business tables plus history. V2/V3 restored unchanged.
- V5 makes SKU/inventory authoritative for price/stock. Original three products retain
  unchanged values in immutable legacy archives. Two explicit demo SKUs have stock 5/0.
- Shared catalogue API and React API client exist; management write repairs are in progress,
  with no UC-13 management screen.
- Orders search/details now read from `hm_order`, `hm_order_item`, `hm_payment`,
  `user_account`, and stored SKU snapshots; this is an explicitly requested integration
  exception, not a claim that UC-18 is complete.
- After integrating develop: 11 isolated PostgreSQL backend tests, 2 frontend tests,
  lint/build/package passed. Legacy upgrade and idempotent migrate/seed passed.
- Live Spring/JDBC catalogue API smoke against Supabase passed. No bootstrap account added.
  Authenticated UI proof against Supabase and hosted CI proof are still pending.

## Current and next task

- Current: Orders read-only query integration with the existing shared tables. No migration
  or shared database modification was added; V7 remains the existing historical migration.
  PostgreSQL integration is not verified because Docker is unavailable in this environment.
- Next: AnNP UC-13 management vertical slice with permissions, version/duplicate checks,
  Vietnamese UI and browser smoke; teammates preserve the existing catalogue API boundaries.
- Open: payment provider selection, Admin/MANAGER wording, BR collisions, iteration ownership, Manager bootstrap for
  authenticated browser proof and RDS synchronization.

## Current task update (2026-10-06)

- Catalog writes use `app.product` metadata and live `app.hm_sku`; no legacy archive,
  inventory or ledger mutation. New SKUs start with available stock zero.
- All `/api/v1/products/{id}` IDs are SKU IDs. Update/delete require productVersion and
  skuVersion and reject stale edits with 409; duplicate codes also return 409.
- Added MANAGER authorization for product create/update/delete at URL and method-security levels.
- Removed timer/microtask workarounds; requests have cancellation and stale-response guards.
  Search remains submit-only, so typing sends no requests. Cart is disabled with an explicit
  unavailable message pending its owner's API; the temporary local cart was removed.
- New security/service tests and PostgreSQL write/read integration tests are added.
  Verification passed: frontend lint, 10 tests and production build; 8 backend security/service
  tests (3 + 5). All PostgreSQL tests compile but have not executed in this review.
  PostgreSQL execution is blocked: Docker is unavailable on this workstation.
- Current task: finish verification/evidence for leader review, then run disposable PostgreSQL
  tests and integrate the approved cart API through its owner before claiming completion.
- Pulled develop through b6b9ad3 and restored local catalog repairs from stash. Resolved
  the PROJECT_STATUS.md conflict by retaining both shared-order integration and catalog status.
  Stash retained as backup; clean build and backend startup after integration are pending.

Low-memory verification commands (repository root, PowerShell; Java 24/Node 25 on this
workstation, not evidence of the pinned Java 21/Node 24 CI environment):

```powershell
$env:NODE_OPTIONS='--max-old-space-size=256'
Push-Location frontend
node node_modules/eslint/bin/eslint.js .
node node_modules/vitest/vitest.mjs run --maxWorkers=1
node node_modules/vite/bin/vite.js build
Pop-Location
$env:MAVEN_OPTS='-Xint -Xms32m -Xmx192m -XX:+UseSerialGC -XX:ActiveProcessorCount=1'
Push-Location backend
.\mvnw.cmd '-Dtest=ProductSecurityTest,ProductServiceTest' '-DforkCount=0' '-Ddebug=false' test
Pop-Location
```

## UC-09 implementation update (2026-10-07)

- Added public product detail metadata/images/eligible SKUs in a read-only repeatable-read
  transaction. Existing APIs retain shapes; no Flyway change or shared DB write.
- Storefront detail URLs use productId, with optional `?sku=skuId` from UC-08. Single SKU
  auto-selects; multiple SKUs support selection/switching, authoritative price/availability,
  quantity validation, image fallback, cancellation, hidden-product 404 and retrieval Retry.
- User confirmed UC-10 is not implemented. Production cart stays disabled; optional async
  adapter receives only skuId/quantity. No UC-10 feature is claimed or implemented.
- Evidence remains in the implementation and tests; the two standalone catalogue Markdown
  documents were removed at the user's request, with temporary local backups retained.
  PostgreSQL test added for real metadata/sibling eligibility/OOS/no reservation, but Docker
  is absent here. Browser proof and real cart integration remain pending.
- Verification passed: frontend lint, all 20 React/client tests and production build;
  12 backend detail/security/service tests (2 + 5 + 5). PostgreSQL tests compile but were
  not executed. Commands are the low-memory commands above, with Maven selection
  `-Dtest=CatalogDetailServiceTest,ProductSecurityTest,ProductServiceTest`.
- Current release gate: do not mark UC-09 fully accepted until disposable PostgreSQL and
  browser checks pass; the cart handoff additionally depends on UC-10's owner.
- Current task: verify this slice. Next: execute the documented disposable PostgreSQL and
  browser checklist, then integrate UC-10 when its API/page exists. RDS synchronization
  remains deferred; no canonical Backlog status was changed.

## Exact verification commands

From repository root, with Node 24/npm 11 and Docker running:

```text
npm run frontend:check
npm run backend:verify:postgres
```

The Orders service unit test also runs without Docker:

```text
.\backend\mvnw.cmd -f backend\pom.xml -B -Dtest=SalesOrderQueryServiceTest test
```

For the configured database with JDK 21 (never run integration tests on shared Supabase):

```text
npm run db:info
npm run db:migrate
npm run db:seed:catalog
```

Expected: Flyway version 5, pending 0. Temporary catalogue preview is localhost:8085;
this is not a team deployment. Stop/remove its homemart-catalog-preview Docker container
when no longer needed. SMTP health is excluded only from this catalogue preview.
