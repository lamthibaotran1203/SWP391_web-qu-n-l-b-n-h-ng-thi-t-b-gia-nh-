# Project status

Updated 2026-10-07. Read this before using planning documents.

## Authority and ownership

- Canonical [35-UC Backlog](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit) governs IDs/ownership/scope.
  Live RDS is work in progress; the historical 37-UC pack is unapproved comparison material.
- User-confirmed AnNP UCs: **04, 05, 13, 15, 32, 33, 34**. Backlog Lead cells updated;
  iteration allocations still need reconciliation. No business UC is claimed complete.
- User-confirmed ThienTP UCs: **27 (Manage Suppliers), 28 (Create Purchase Order)**.
  Both vertical slices implemented and verified with unit tests and UI integration.
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
- **UC-27 (Manage Suppliers)** implemented:
  - Backend: Supplier entity, status enum, repository, validation service with duplicate code & tax code guards, DTOs, controller at `/api/v1/purchasing/suppliers`.
  - Frontend: `features/purchasing/api.js`, `SuppliersPage.jsx` with Vietnamese UI, search, status filter, registration & edit modals, soft-delete.
  - Tests: `SupplierServiceTest` (6/6 passed), `SuppliersPage.test.jsx` (3/3 passed).
- **UC-28 (Create Purchase Order)** implemented:
  - Backend: PurchaseOrder & PurchaseOrderItem entities mapping `app.hm_purchase_order` & `app.hm_po_item`, status enum, validation service with whole-VND precision (BR-37), duplicate SKU prevention, minimum 1 item requirement (28.0.E3), active supplier verification (BR-27), controller at `/api/v1/purchasing/purchase-orders`.
  - Invariant POST-5 strictly preserved: Physical inventory is NOT increased at PO creation.
  - Invariant BR-28 strictly preserved: `received_quantity = 0 <= ordered_quantity`.
  - Frontend: `CreatePurchaseOrderPage.jsx` with supplier selection, dynamic SKU table, live VND subtotal calculation, draft vs confirmation dispatch.
  - Invalid expected dates are rejected instead of being silently removed; fractional VND costs
    are rejected instead of silently rounded.
  - Tests: `PurchaseOrderServiceTest` (8/8 passed), `CreatePurchaseOrderPage.test.jsx` (3/3 passed).
- Repository audit fixes on 2026-10-06:
  - Every current React route is forwarded to `index.html` in the packaged SPA, so direct refresh
    no longer fails outside `/` and `/login`.
  - Legacy `/api/v1/products` writes are denied because V5 makes `legacy_*` columns immutable;
    UC-13 must add a canonical product/SKU write contract before those writes are reopened.
  - Supplier/PO paging, IDs, keywords, supplier email, and whole-VND inputs now have server-side
    boundary validation. Catalogue prices render without JavaScript safe-integer truncation.
- Frontend test suite: 13/13 tests passed, ESLint 0 errors, production build clean.
- Affected backend unit suite: 22/22 tests passed, 0 failures, including two new PO regression
  tests and one SPA-forward regression test.
- Spring Boot JAR packaging passed with the already-built React assets.
- Existing frontend screens now share one HomeMart UI system derived from the reviewed MeGo
  interaction patterns without copying its business logic or Ant Design dependency. The shared
  shell covers brand navigation and role-aware actions; home, authentication, catalogue, staff
  orders, suppliers, and purchase-order screens use consistent color, spacing, cards, forms,
  tables, modals, feedback states, and responsive behavior. Frontend lint, 13/13 tests, and the
  production build pass after the UI refresh; local browser checks covered desktop-style and
  narrow layouts with no console rendering errors. Backend-dependent data remained unavailable
  during visual QA because only the frontend preview was running.

## Current and next task

- Current: integrate the verified project-wide HomeMart UI, UC-27 and UC-28 with the latest
  `develop`, including UC-05, UC-08, UC-09, UC-13 and the Orders read model.
- Next: complete the combined frontend and disposable PostgreSQL verification, then submit
  `develop-thien` for review into `develop`.
- Open: payment provider selection, Admin/ADMIN wording, BR collisions, iteration ownership, Admin bootstrap for
  authenticated browser proof and RDS synchronization.

## Current task update (2026-10-06)

- Catalog writes use `app.product` metadata and live `app.hm_sku`; no legacy archive,
  inventory or ledger mutation. New SKUs start with available stock zero.
- All `/api/v1/products/{id}` IDs are SKU IDs. Update/delete require productVersion and
  skuVersion and reject stale edits with 409; duplicate codes also return 409.
- Added ADMIN authorization for product create/update/delete at URL and method-security levels.
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

## MR !11 integration update (2026-10-07)

- Merged `origin/develop` through `92eb44c` into the UC-13 branch in an isolated worktree.
  Existing storefront, account and Orders UI changes from `develop` were retained.
- UC-13 remains the canonical catalog write path at `/api/v1/admin/catalog/products`.
  Legacy `/api/v1/products` writes remain retired with HTTP 410; public reads retain the
  product/SKU version fields added on `develop`.
- Conflict resolution removed duplicated Orders styles from `main.css`; Orders continues to
  use its dedicated `orders.css`. No changes were made in the active UI working tree.
- Verification passed: frontend lint, 31 React/client tests and production build; disposable
  PostgreSQL verification ran all 53 backend tests with zero failures and packaged the JAR.

## `develop-thien` integration update (2026-10-07)

- Merged `origin/develop` through `4453656` into the HomeMart UI branch and resolved the
  remaining SPA route, catalog client, product list/detail, stylesheet and status conflicts.
- The HomeMart shell and visual system remain active. Product cards now navigate with canonical
  `productId` plus the selected `skuId`; product detail retains UC-09 multi-SKU behavior.
- SPA forwarding covers UC-05, UC-13, UC-27 and UC-28 routes together with existing public and
  Orders routes. Legacy catalog writes remain retired in favor of the admin catalog API.
- Combined verification passed: frontend lint, all 37 React/client tests and production build;
  disposable PostgreSQL verification ran all 68 backend tests with zero failures and packaged
  the Spring Boot JAR.

## Exact verification commands

From repository root, with Node 24/npm 11 and Docker running:

```text
npm run frontend:check
npm run backend:verify:postgres
```

The affected backend unit suite can be run without Docker:

```text
.\backend\mvnw.cmd -f backend\pom.xml -Dtest=AccountProfileServiceTest,BootstrapAdminServiceTest,SalesOrderQueryServiceTest,PurchaseOrderServiceTest,SupplierServiceTest,SpaForwardControllerTest test
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

The last verified shared Supabase state was Flyway version 5 with pending 0. The repository now
also contains V6 and V7, so check `db:info` and team ownership before migrating the shared database.
Temporary catalogue preview is localhost:8085;
this is not a team deployment. Stop/remove its homemart-catalog-preview Docker container
when no longer needed. SMTP health is excluded only from this catalogue preview.
