# Project status

Updated 2026-10-06. Read this before using planning documents.

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
  - Tests: `PurchaseOrderServiceTest` (6/6 passed), `CreatePurchaseOrderPage.test.jsx` (3/3 passed).
- Frontend test suite: 13/13 tests passed, ESLint 0 errors, production build clean.
- Backend unit test suite: 19/19 tests passed, 0 failures.

## Current and next task

- Current: UC-27 and UC-28 full vertical slices (Backend + Frontend + Tests) implemented and integrated with Orders read model fix.
- Next: Integration review with teammates, goods receiving flow (UC-29) prerequisite contracts.
- Open: payment provider selection, Admin/MANAGER wording, BR collisions, iteration ownership, Manager bootstrap for
  authenticated browser proof and RDS synchronization.

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
