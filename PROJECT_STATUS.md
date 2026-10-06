# Project status

Updated 2026-10-07. Read this before using planning documents.

## Authority and ownership

- Canonical [35-UC Backlog](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit) governs IDs/ownership/scope.
  Live RDS is work in progress; the historical 37-UC pack is unapproved comparison material.
- User-confirmed AnNP UCs: **04, 05, 13, 15, 32, 33, 34**.
- User-confirmed ThienTP UCs: **27 (Manage Suppliers), 28 (Create Purchase Order)**.
  Both vertical slices implemented and verified with unit tests and UI integration.

## Release gate and completed evidence

- R1 walking skeleton was locally verified earlier. Shared Supabase uses Flyway V1–V5;
  zero pending migrations, 24 business tables plus history. V2/V3 restored unchanged.
- V5 makes SKU/inventory authoritative for price/stock. Original three products retain
  unchanged values in immutable legacy archives. Two explicit demo SKUs have stock 5/0.
- Shared read-only catalogue API and React API client exist; no UC-13 management screen
  or write flow yet. [Team integration contract](docs/contracts/Catalog_Shared_Contract.md).
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

- Current: project-wide frontend UI refresh is implemented and verified while preserving existing
  API calls, authorization, validation, and business flows.
- Next: run the PostgreSQL integration suite and smoke flow after Docker Desktop is available,
  then reconcile the unused V7 `sales_order*` read-model tables with the current `hm_order*` implementation.
- Open: payment provider selection, Admin/MANAGER wording, BR collisions, iteration ownership, Manager bootstrap for
  authenticated browser proof and RDS synchronization. Local integration verification is blocked
  because Docker Desktop does not currently start on this workstation. The root build script also
  requires the pinned Node 24/npm 11; the active shell currently exposes Node 22/npm 10.

## Exact verification commands

From repository root, with Node 24/npm 11 and Docker running:

```text
npm run frontend:check
npm run backend:verify:postgres
```

The affected backend unit suite can be run without Docker:

```text
.\backend\mvnw.cmd -f backend\pom.xml -Dtest=AccountProfileServiceTest,BootstrapManagerServiceTest,SalesOrderQueryServiceTest,PurchaseOrderServiceTest,SupplierServiceTest,SpaForwardControllerTest test
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
