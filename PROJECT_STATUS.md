# Project status

Updated 2026-10-05. Read this before using planning documents.

## Authority and ownership

- Canonical [35-UC Backlog](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit) governs IDs/ownership/scope.
  Live RDS is work in progress; the historical 37-UC pack is unapproved comparison material.
- User-confirmed AnNP UCs: **04, 05, 13, 15, 32, 33, 34**. Backlog Lead cells updated;
  iteration allocations still need reconciliation. No business UC is claimed complete.
- Shared catalogue decisions recorded in Backlog I14/J14. RDS updates deferred by the user;
  local notes, draft diagram SQL and screenshots are ignored, not fresh-clone dependencies.

## Release gate and completed evidence

- R1 walking skeleton was locally verified earlier. Shared Supabase uses Flyway V1–V5;
  zero pending migrations, 24 business tables plus history. V2/V3 restored unchanged.
- V5 makes SKU/inventory authoritative for price/stock. Original three products retain
  unchanged values in immutable legacy archives. Two explicit demo SKUs have stock 5/0.
- Shared read-only catalogue API and React API client exist; no UC-13 management screen
  or write flow yet. [Team integration contract](docs/contracts/Catalog_Shared_Contract.md).
- After integrating develop: 11 isolated PostgreSQL backend tests, 2 frontend tests,
  lint/build/package passed. Legacy upgrade and idempotent migrate/seed passed.
- Live Spring/JDBC catalogue API smoke against Supabase passed. No bootstrap account added.
  Authenticated UI proof against Supabase and hosted CI proof are still pending.

## Current and next task

- Current: publish shared schema/catalogue prerequisites through MR An -> develop;
  keep intermediate review files local. Existing unrelated ok.md deletion stays outside MR.
- Next: AnNP UC-13 management vertical slice with permissions, version/duplicate checks,
  Vietnamese UI and browser smoke; teammates integrate through the shared catalogue contract.
- Open: payment provider selection, Admin/MANAGER wording, BR collisions, iteration ownership,
  Manager bootstrap for authenticated browser proof and RDS synchronization.

## Exact verification commands

From repository root, with Node 24/npm 11 and Docker running:

```text
npm run frontend:check
npm run backend:verify:postgres
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
