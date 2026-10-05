# Project status

Schema deployment and documentation review: 2026-10-05. Application runtime evidence below was last recorded on
2026-09-14; it has not been reverified against teammates' later work. The isolated PostgreSQL
diagram verification on 2026-10-05 is documented separately below.

## Requirements and document status

- The [35-UC Google Sheet Backlog](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit) is the current canonical register for top-level UC IDs, names, modules, actors, and iteration ownership. Make UC name/actor/scope corrections there first, then propagate them to the RDS and diagrams. The [live RDS](https://docs.google.com/document/d/1Qc5Kk2D8MX09AV_7Z4iEg2fsXBElREgomhF-0MBxrDA/edit) is work in progress and is not necessarily the submitted revision.
- The [UC Diagram feedback note](https://docs.google.com/document/d/1ryJWN3UKQ2QCc-ojVayblENErgvZZwbcDGYQb_T75dg/edit) is the team's transcription of oral feedback, not a lecturer-authored comment record. It calls for changes to actors, UC boundaries/names, and diagram relationships. Original lecturer-authored comments are preserved separately in `Feedback Gvien/Feedback_15 09.docx` on Google Drive.
- `household-goods-sales-planning-handoff/` is an earlier **unapproved 37-UC proposal**. It conflicts with the submitted 35-UC scope, including Supplier/Purchase Order and the role model. Its IDs, counts, exclusions, and `PLAN BASELINE` decisions must not silently replace the submitted documents.
- Keep the 35 submitted IDs as traceability anchors while reviewing them. A revised count, name, actor, scope, or ownership requires an explicit team decision and a mapping back to the submitted version. Current code/migrations establish implementation status only.
- The Backlog currently assigns only 34 of 35 UCs across Iter1-Iter4: UC-20 is missing. Its top-level Lead/Reviewer cells remain `Unassigned`, and the Contribution sheet still contains placeholder task text. Resolve these in the Sheet rather than inventing commit history in the RDS.
- Still unresolved for the revised RDS: actor/role naming (`Admin` versus implemented `MANAGER`), session versus JWT wording, UC-18/19/20/21/24/25/29/31/32 boundaries, stock/order/payment lifecycle consistency, and physical schema compatibility with PostgreSQL. Supplier/procurement remains in the current 35-UC baseline unless the team explicitly removes it.
- `docs/HomeMart_RDS_Consolidated_Review.docx` is the current local review guide for actor-based UC Diagrams, Screen Flow, ERD, physical database design, and lecturer feedback. It contains the full 35-UC review, proposed actor associations, the working `include`/`extend` registry with arrow directions, screen-flow corrections, ERD cardinalities, PostgreSQL schema guidance, and the unresolved decisions. It does not replace the canonical Backlog or the live RDS; proposals and conflicts must be decided in the Sheet before propagation.
- `docs/Staff_Admin_Screen_Flow_Review.drawio` is the editable local Screen Flow review with exactly two actor pages: one Staff flow and one Admin flow. Each page uses one Home Page, one Login and one Dashboard. The canvas uses only monochrome rectangles for screens and ovals for dialogs; it contains no free-floating module headings, UC numbers, legends, or explanatory notes. Every connector names its triggering action. `docs/Staff_Admin_Screen_Flow_Review.md` records the navigation rules and detailed rationale outside the diagram.

## Current release gate

R1 executable walking skeleton is locally complete. React, Spring Security, Flyway,
PostgreSQL, packaging and browser proof exist. The shared Supabase PostgreSQL database is now
connected through Spring, migrated by Flyway and validated by Hibernate; Supabase login proof
awaits a locally configured bootstrap Manager. CI-host proof is still pending.

## User-owned use cases

User-confirmed AnNP scope on 2026-10-05: **UC-04, UC-05, UC-13, UC-15, UC-32, UC-33,
UC-34**. The canonical Backlog's seven top-level Lead cells now record AnNP. Iteration sheets
still need reconciliation by the team. No business UC implementation is claimed.

## Current catalog foundation task — 2026-10-05

- Backlog J14 records the user-approved shared product/SKU integration contract; live RDS edits
  deferred by the user. `docs/contracts/Catalog_Shared_Contract.md` is the concrete handoff.
- Applied V5 through Flyway on Supabase; original three products' values preserved as immutable
  legacy archives. Live price/stock sources are SKU/inventory, with no two-way synchronization.
- Shared public read API, reference DTOs and frontend client exist; this is a prerequisite,
  not completion of UC-09/10 or UC-13 management. Visibility includes active product/SKU and
  undeleted references; missing stock is 0, zero stock visible/unpurchasable, money is string.
- Explicit idempotent demo seed adds two labelled products/SKUs (stock 5/0) without accounts.
- Verification: frontend lint/build + 2 tests; isolated PostgreSQL backend clean verify + 11
  tests; local V1–V3 -> V4 -> V5 archive-preserving upgrade and zero-migration rerun.
- Live Spring/JDBC catalogue API smoke uses Supabase without bootstrap accounts. Temporary
  Docker preview is localhost:8085; it is not an external deployment or team-host proof.
- Next: implement AnNP UC-13 management vertical slice with version/permission/duplicate tests;
  teammates consume the shared contract. RDS checklist kept separately as requested.

## Current schema deployment task

- On 2026-10-05, discovered Supabase already had Flyway V1–V3 and three products, rather
  than an empty database. Restored exact applied V2/V3 from GitLab history; did not edit them.
- Recorded schema/lifecycle decisions in canonical Backlog column I before propagation.
- Applied **V4** to Supabase PostgreSQL 17.6 through Flyway: **24 business tables plus
  Flyway history**, current version 4, zero pending migrations. No account or demo fixture added.
- Remote rerun executed zero migrations; original-column digest matched. Three SKU/inventory
  rows preserve total stock 65 and prices; three opening entries, no duplicate identity tables.
- Kept `app.user_account` and existing `app.product`; added business contracts, FK/indexes,
  default-address/publication/return-line protections, immutable history and financial evidence.
- Existing products preserve original values except intentional ACTIVE -> INACTIVE for
  missing brand/image; SKU/inventory/opening ledger projections preserve existing price/stock.
- Historical diagram SQL remains outside Flyway and must not be applied to shared Supabase.
- Evidence, recorded decisions, RDS synchronization addendum and integration limitations:
  `docs/database/HomeMart_Schema_Deployment_2026-10-05.md`.
- Local Java 21/Maven 3.9.16/PostgreSQL 17 verification: **7 integration tests passed**,
  Hibernate validation and packaging passed. Separate V1–V3 -> V4 legacy-data upgrade passed;
  repeat migration executed zero. Frontend lint, one test and production build passed.
- Runtime login/browser proof against Supabase was not repeated; no business feature is
  complete merely because the schema is deployed. Live RDS/ERD still need synchronization.

## Completed evidence

- Monorepo layout, pinned Java/Node/Maven versions, wrappers, ignored environment sample,
  editor/line-ending rules and GitLab templates.
- Vite `/api` proxy and production React output copied into the Spring Boot JAR.
- Real CSRF endpoint, CSRF-protected login/logout, server session and safe current-user DTO.
- Environment-driven, BCrypt-hashed, idempotent first-Manager bootstrap with no committed
  default credentials.
- Flyway `V1` creates `app.user_account`; Hibernate validates the PostgreSQL mapping.
- Stable problem response and per-request trace ID foundation.
- Frontend lint/test/build passed on 2026-09-10.
- PostgreSQL 17 integration tests passed under Java 21/Maven 3.9.16: 2 tests, 0 failures,
  0 errors.
- Authentication tests use an explicit `.invalid` fixture and remove it after the test class;
  production UI contains no test credentials or walking-skeleton/debug copy.
- Development browser smoke passed through the Vite `/api` proxy: connected state, login,
  refresh-preserved session, CSRF logout, anonymous state, empty local/session storage and no
  console errors.
- Packaged Spring Boot JAR browser smoke passed with the embedded React build and no console
  errors.
- GitLab CI YAML parsed successfully; the executable JAR was packaged again under the pinned
  Java 21/Maven 3.9.16 toolchain and contains the React entry point/assets.
- Supabase developer MCP is scoped and OAuth-authenticated to the selected project; GitLab
  `origin`, local SMTP variables, Spring Mail configuration and secret-file guards are
  prepared without exposing credentials to React or tracked configuration.
- A read-only Supabase query confirmed PostgreSQL 17.6, database/user `postgres`, and no
  existing `app` schema. Direct IPv6 and Supavisor session-mode endpoints are reachable.
- On 2026-09-14, Spring Boot connected to Supavisor session mode over SSL, Flyway created
  schema `app` and applied `V1`, Hibernate validated the mapping, and `/actuator/health` plus
  `/api/v1/auth/csrf` responded successfully. No Manager was created because bootstrap values
  were intentionally absent from the local environment.
- Online payment is explicitly deferred; no provider-specific runtime configuration or
  implementation is retained. The local `.env` is ignored and restricted to the current OS
  user.

## Last recorded implementation task

Standardize and deploy catalogue sources/API/demo through V5; preserve applied history and legacy data.

## Next recorded implementation task

Implement assigned UC-13 management against the shared catalogue contract. RDS updates are
deferred by user request; bootstrap Manager is still needed for authenticated browser proof.

## Blockers and unresolved decisions

- Supabase MCP, network reachability and Spring/Flyway proof are verified against PostgreSQL
  17.6. Session pooler is the tested connection choice. Browser authentication proof requires
  a locally configured bootstrap Manager (DEC-27, DEC-38).
- SMTP credentials are available locally; a real-message proof and credential rotation remain
  pending (DEC-03).
- UC-15 is assigned to AnNP. Payment schema is ready; provider selection and payment flow
  implementation remain open (DEC-04), with no active provider configuration.
- GitLab `origin` is configured, but repository authentication, protected `main`, masked CI/CD
  variables and the first hosted pipeline are pending.
- Deployment host (DEC-29) and team ownership mapping (DEC-30) remain unresolved.

## Exact verification commands

From the repository root with JDK 21, Node 24/npm 11 and Docker running:

```text
npm run db:up
npm run frontend:check
npm run backend:test
npm run build
```

Visible result: `http://localhost:5173` shows “Hệ thống đã sẵn sàng”; a configured bootstrap
Manager can log in, survive refresh, log out through CSRF, and becomes anonymous on the next
`/api/v1/auth/me` request.

## Schema-only commands (macOS/Linux and Windows PowerShell)

From repository root with JDK 21 and the ignored local DB environment configured:

```text
npm run build
npm run db:info
npm run db:migrate
npm run db:info
```

These commands run Flyway without web startup, mail or account bootstrap. Expected after
V5: current version 5, pending 0, 25 app tables including Flyway history. Do not run integration
tests against shared Supabase; use the isolated PostgreSQL test database.
