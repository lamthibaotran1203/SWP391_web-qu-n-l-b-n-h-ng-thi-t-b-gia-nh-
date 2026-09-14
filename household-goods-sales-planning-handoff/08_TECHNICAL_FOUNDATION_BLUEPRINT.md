# Technical Foundation Blueprint

Status: **planned architecture, API, screen, database, security, CI, and test contract**

This file bridges approved requirements to RDS and implementation. It defines boundaries and
evidence, not final Java class names or a physical SDS. Actual symbols may differ after
implementation, but behavior, authorization, data invariants, and traceability may not drift
without an approved decision change.

## 1. Architecture decision

Use a modular monolith with one production origin and one deployable artifact.

```text
Development
  React/Vite :5173 --proxy /api--> Spring Boot :8080 --> PostgreSQL

Production/demo
  Browser --HTTPS--> Spring Boot
                        |-- serves React static bundle and SPA fallback
                        |-- exposes /api/v1
                        +-- JDBC/SSL --> Supabase PostgreSQL app schema

External supporting systems
  Spring Boot --> SMTP sandbox
  Browser/Spring Boot <--> one payment sandbox
```

### Why this boundary

- Satisfies ReactJS and Spring MVC while retaining one deployment and one security origin.
- Avoids JWT access/refresh/revocation and cross-site cookie/CORS complexity.
- Keeps all database credentials, authorization, calculations, and transactions on the server.
- Lets the React client be replaced without changing domain services or the schema.
- Leaves a clean Controller -> Service -> Repository sequence for RDS/SDS diagrams.

### Prohibited shortcuts

- React must not call Supabase Data API or embed a database/service key.
- Controllers must not contain stock/payment/refund transaction logic.
- Repositories must not decide authorization or lifecycle transitions.
- JPA entities must not be serialized directly to the browser.
- Frontend route guards must not be treated as security.
- `ddl-auto=update/create` must not own schema outside disposable experiments.
- H2 success must not be claimed as PostgreSQL integration proof.

## 2. Repository blueprint

```text
/
  backend/
    pom.xml
    src/main/java/<group>/<project>/
    src/main/resources/
      application.yml
      db/migration/
    src/test/
  frontend/
    package.json
    package-lock.json
    vite.config.*
    src/
    public/
  docs/
    planning/
    rds/
    sds/
    diagrams/
    evidence/
  scripts/
    build-all.*
    run-local.*
    seed-demo.*
  .gitlab/
    issue_templates/
    merge_request_templates/
  .gitlab-ci.yml
  .editorconfig
  .gitignore
  README.md
```

`frontend/dist` and secrets are generated/ignored. The production package task builds the
frontend first and includes its output as Spring Boot static resources. A SPA fallback serves
`index.html` for non-API client routes but never converts unknown `/api/**` paths into HTML.

## 3. Backend structure

Use package-by-feature inside one Spring Boot application. Suggested top-level boundaries:

| Package boundary | Responsibility |
| --- | --- |
| `common` | API errors, paging, time/money helpers, request correlation |
| `config` | Spring/Jackson/JPA/application configuration |
| `security` | session login/logout, CSRF, role rules, current actor |
| `identity` | account, token, profile/address, Staff management |
| `catalog` | Category, Brand, Product, SKU, public/admin projections |
| `order` | checkout, order snapshots, customer/staff enquiries, cancellation |
| `payment` | attempts, provider adapter, result verification, money ledger |
| `inventory` | balances, receipts, adjustments, movements |
| `fulfillment` | queue, pack, dispatch, delivery outcome, counter sale orchestration |
| `returns` | request, review, intake/disposition, refund eligibility |
| `report` | cash, completed-sales, and inventory roll-forward read models |

Within a feature, controllers depend on application services; services depend on repositories
and focused provider ports. Cross-feature calls use application services or small domain
policies, not controller-to-controller calls. `@Transactional` belongs on service commands and
report queries where a consistent read is required.

### Layer contracts

| Layer | May do | Must not do |
| --- | --- | --- |
| REST Controller | parse DTO, invoke service, map status/response | calculate authoritative totals, mutate entities directly |
| Application Service | authorize ownership, lock/revalidate, transition state, commit transaction | return persistence entity graph to client |
| Repository | load/lock/store/query data with explicit projection | decide actor permissions or business workflow |
| Provider Adapter | build/verify provider protocol outside long DB lock | directly change order/payment state |
| Scheduler | find due candidates and call idempotent service command | impersonate a human actor or bypass guards |

## 4. Frontend structure

Use React with Vite, React Router, one shared API client, Bootstrap plus small project CSS,
Vitest, and React Testing Library. Avoid Redux, Next.js, server-side rendering, and a large
component framework unless a demonstrated need appears.

Suggested boundaries:

```text
src/
  app/             router, providers, layout, error boundary
  api/             fetch client, problem-response mapping, CSRF refresh
  auth/            current-user context and route helpers
  cart/            versioned localStorage model and validation
  features/        identity, catalog, order, inventory, fulfillment, returns, reports
  components/      small shared controls only
  pages/           route-level compositions
  test/            fixtures and render helpers
```

### Client-state rules

- Auth truth comes from `GET /api/v1/auth/me`; role hiding is presentational.
- Cart stores only SKU ID/code, desired quantity, and display snapshot. Checkout sends SKU
  IDs/quantities; server ignores client price/total.
- Cart storage includes a schema version. Invalid/stale data is discarded safely.
- Server resources are reloaded after successful mutation; do not manually fabricate a new
  order/payment/stock state in React.
- Forms display field errors from a stable error contract and retain safe user input.
- Mutating buttons disable while pending, but server idempotency/state guards remain required.

## 5. Session and CSRF security contract

### Session model

- Spring Security owns authentication and creates an HttpOnly session cookie.
- Production cookie: Secure over HTTPS, SameSite=Lax, no JavaScript access.
- Successful login changes the session ID to prevent fixation.
- Logout is POST and invalidates server session.
- Password change/reset and Staff deactivation invalidate all active sessions for that user.
- Idle timeout is 30 minutes and remember-me is disabled.
- One running application instance with an account-to-session registry is the baseline, so
  credential/status commands can expire that account's sessions. Distributed session storage
  and multi-instance deployment are excluded.

### SPA CSRF flow

1. React loads `GET /api/v1/auth/me` or `GET /api/v1/auth/csrf` and receives/refreshes a CSRF
   token according to the pinned Spring Security SPA configuration.
2. The API client sends the token header for POST/PUT/PATCH/DELETE.
3. Login and logout clear/rotate CSRF state; React obtains a fresh token before the next
   unsafe request.
4. Missing/invalid CSRF receives 403 and no business mutation.

Public browser forms such as register/login/reset still obtain and send CSRF. Only the exact
external provider callback/IPN route may be exempt, because it uses provider signature and
reference/amount verification instead; no broad `/payments/**` CSRF bypass is allowed.

### Authorization rules

- Public allow-list is explicit: static assets, catalogue reads, register/verify/login/reset,
  payment provider callback/return as required, and health endpoint with no secrets.
- `/api/v1/me/**` requires Customer for customer-owned resources unless the endpoint is an
  authenticated-user identity operation.
- `/api/v1/staff/**` allows Staff/Manager.
- `/api/v1/manager/**` allows Manager.
- Services re-check ownership, target role, and current state. URL prefix alone is not proof.
- Customer-owned lookup includes the authenticated Customer in the repository predicate and
  returns 404 for absent/not-owned business codes, preventing existence disclosure.
- DTO output is allow-listed; no password hash, token hash, internal exception, or secret.
- Production needs no credentialed cross-origin CORS. Development uses the Vite proxy; never
  combine wildcard origins with session credentials.

### Security non-goals

No MFA, OAuth/social login, API keys for human users, device management, Redis-backed rate
limiting, JWT blacklist, Supabase Auth, or browser RLS context. Basic login throttling may be
added only as a technical hardening task without a new UC.

## 6. API conventions

- Base path: `/api/v1`.
- JSON uses camelCase; database uses lowercase snake_case.
- Resource paths use nouns. Business commands use clear subresources such as `/pack` or
  `/cancellation`, not a generic status update endpoint.
- Customer-facing order/return paths use business codes, not sequential database IDs.
- Timestamps are ISO-8601 instants. Date filters are `YYYY-MM-DD` local business dates and
  the server converts them to half-open instant ranges.
- Whole-VND values are nonnegative integral JSON numbers within `numeric(14,0)` range.
- Paged endpoints accept `page`, `size`, and an allow-listed sort; default size 20, maximum 50.
- Every stable sort ends with the primary key as a tie-breaker.
- Creation commands UC-12, UC-22, UC-29, UC-30, and UC-31 use an
  `Idempotency-Key` UUID when duplicate creation would be harmful.
- Mutable aggregate commands send expected `version` where stale UI state can apply.
- UC-12 includes selected `addressId` and expected address `version`; the backend locks the
  owned active address before SKU rows and never trusts address text supplied by React.
- Database transaction baseline is PostgreSQL `READ COMMITTED` plus explicit pessimistic row
  locks for high-risk commands, unique/CHECK constraints, and optimistic `version` conflicts;
  Java-only locks and a blanket SERIALIZABLE setting are not accepted substitutes.
- The server computes SHA-256 over one documented canonical command representation and stores
  it as `request_fingerprint`; the browser never supplies a trusted fingerprint. Sorted
  normalized line IDs/quantities and all result-changing fields are included.

### Response shapes

Single result:

```json
{
  "data": {}
}
```

Paged result:

```json
{
  "data": [],
  "page": { "number": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
}
```

Error result uses RFC-style problem fields plus stable project details:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "code": "ORDER_STATE_CONFLICT",
  "detail": "Đơn hàng không còn ở trạng thái cho phép.",
  "fieldErrors": {},
  "traceId": "opaque-correlation-id"
}
```

Expected mapping: validation 400, unauthenticated 401, forbidden 403, missing 404,
state/version/idempotency conflict 409, external dependency unavailable 503, and unexpected
failure 500 with no internal detail.

### Input and field boundaries

React hints and validates for usability; Bean Validation/service rules are authoritative and
Flyway CHECK/length constraints match where row-local enforcement is possible.

- Trim ordinary text, reject blank/control-only input, and render user text escaped. Email is
  trimmed/lowercased and at most 254 characters.
- Full/recipient names are 2–100 characters. Baseline phone is exactly ten decimal digits
  beginning with `0`. A delivery address is one 10–500 character Vietnamese address string;
  there is no external administrative-address API.
- Password is 8–64 characters with at least one letter and digit and at most 72 UTF-8 bytes.
- Category/Brand names are 2–100; Product name 2–150; description up to 2,000; immutable SKU
  code matches `[A-Z0-9-]{3,40}`; sale-unit label is 1–100; image URL is HTTPS and at most 500.
- Cart/checkout/counter contain at most 50 distinct SKUs and quantity 1–999 per line. Stock
  receipt contains at most 100 distinct SKUs and quantity 1–999,999 per line.
- Required operational reason/reference/note text uses a documented 1–500 bound. Public
  business codes are server-generated; `Idempotency-Key` is a UUID.
- Report start/end dates are required, start may not exceed end, and an accepted range is at
  most 366 local dates. Larger analytics/export scope is excluded.

RDS screen field tables must copy these boundaries or explicitly record an approved change;
React, DTO validation, service tests, and DDL may not each invent a different limit.

## 7. API route catalogue

Supporting routes are marked `SUPPORT`; they do not create extra UCs.

| UC | Method and path | Authorization | Result |
| --- | --- | --- | --- |
| UC-01 | `POST /api/v1/auth/register` | Public | pending Customer and verification dispatch result |
| UC-01 | `POST /api/v1/auth/verify-email` | Public token | active verified account |
| UC-01 | `POST /api/v1/auth/resend-verification` | Public generic response | new eligible token dispatch |
| UC-02 | `POST /api/v1/auth/login` | Public + CSRF rule | session and user summary |
| UC-03 | `POST /api/v1/auth/logout` | Authenticated | invalidated session |
| SUPPORT | `GET /api/v1/auth/me`, `GET /api/v1/auth/csrf` | Public-safe/session-aware | current actor and fresh CSRF state |
| UC-04 | `GET`, `PUT /api/v1/me/profile` | Customer | own safe profile projection/update |
| UC-05 | `GET`, `POST /api/v1/me/addresses` | Customer | own address list/create |
| UC-05 | `PUT /api/v1/me/addresses/{id}` | Customer owner | update address |
| UC-05 | `POST /api/v1/me/addresses/{id}/default`, `DELETE .../{id}` | Customer owner | set default or deactivate |
| UC-06 | `PUT /api/v1/account/password` | Authenticated | password replaced and session policy applied |
| UC-07 | `POST /api/v1/auth/password-reset/request` | Public | generic dispatch response |
| UC-07 | `POST /api/v1/auth/password-reset/confirm` | Public token | password replaced |
| UC-08 | `GET`, `POST /api/v1/manager/staff-accounts` | Manager | paged Staff list/create setup |
| UC-08 | `PUT /api/v1/manager/staff-accounts/{id}` | Manager, Staff target only | safe identity/status update |
| UC-08 | `POST .../{id}/setup-token` | Manager, Staff target only | new setup dispatch result |
| UC-09 | `GET /api/v1/catalog/products` | Public | active paged catalogue |
| UC-10 | `GET /api/v1/catalog/products/{productId}` | Public | active product/SKU detail |
| SUPPORT | `GET /api/v1/catalog/filters`, `GET /api/v1/storefront/config` | Public safe data | active filter options and server-owned fee/method/policy display values |
| UC-11 | no mutation API; React cart uses catalogue reads | Public | browser-local cart state |
| UC-12 | `POST /api/v1/orders` | Customer + idempotency | new reserved DELIVERY order using COD/ONLINE payment |
| UC-13 | `POST /api/v1/me/orders/{code}/payment-attempts` | Customer owner | provider redirect/attempt |
| UC-13 | provider-specific return/IPN under `/api/v1/payments/{provider}/...` | Signed external/public protocol | verified idempotent finalization |
| UC-14 | `GET /api/v1/me/orders` | Customer | owned paged order list |
| UC-15 | `GET /api/v1/me/orders/{code}` | Customer owner | detail/timeline/actions/receipt projection |
| UC-16 | `POST /api/v1/me/orders/{code}/cancellation` | Customer owner | guarded cancellation |
| UC-17 | `GET /api/v1/staff/orders` | Staff/Manager | operational paged search with actionable fulfillment preset |
| UC-18 | `GET /api/v1/staff/orders/{code}` | Staff/Manager | operational detail/actions/receipt |
| UC-19 | `GET`, `POST /api/v1/manager/categories`; `PUT .../{id}`; `POST .../{id}/activation` | Manager | list/create/edit/activate or guarded deactivate Category |
| UC-19 | same method pattern under `/api/v1/manager/brands` | Manager | list/create/edit/activate or guarded deactivate Brand |
| UC-20 | `GET`, `POST /api/v1/manager/products`; `GET`, `PUT .../{id}`; `POST .../{id}/activation` | Manager | list/create/detail/edit/activate or deactivate Product |
| UC-20 | `POST .../products/{id}/skus`; `PUT .../skus/{skuId}`; `POST .../skus/{skuId}/activation` | Manager | create/edit/activate or deactivate owned SKU |
| UC-21 | `GET /api/v1/staff/inventory` | Staff/Manager | current balances/low-stock filter |
| UC-22 | `POST /api/v1/staff/stock-receipts` | Staff/Manager + idempotency | atomic receipt and movements |
| UC-23 | `POST /api/v1/manager/inventory/{skuId}/adjustments` | Manager + version | guarded correction/movement |
| UC-24 | `GET /api/v1/manager/inventory-movements` | Manager | immutable movement search |
| UC-25 | `POST /api/v1/staff/orders/{code}/pack` | Staff/Manager + version | `CONFIRMED -> PACKING` |
| UC-26 | `POST /api/v1/staff/orders/{code}/dispatch` | Staff/Manager + version | stock departure and `SHIPPING` |
| UC-27 | `POST /api/v1/staff/orders/{code}/delivery-outcome` | Staff/Manager + version | completion/COD or returned failure |
| UC-28 | `POST /api/v1/manager/orders/{code}/cancellation` | Manager + version | pre-shipping cancellation with reason |
| UC-29 | `POST /api/v1/staff/counter-sales` | Staff/Manager + idempotency | completed cash sale and receipt |
| UC-30 | `POST /api/v1/me/orders/{code}/returns` | Customer owner + idempotency | one CUSTOMER-channel return request |
| UC-31 | `POST /api/v1/staff/returns/in-store` | Staff/Manager + idempotency | one receipt-based return request |
| SUPPORT | `GET /api/v1/staff/returns`, `GET /api/v1/staff/returns/{returnCode}` | Staff/Manager | return queue/detail |
| UC-32 | `POST /api/v1/manager/returns/{returnCode}/decision` | Manager + version | approve all or reject |
| UC-33 | `POST /api/v1/staff/returns/{returnCode}/receipt` | Staff/Manager + version | one intake/disposition and stock restore |
| UC-34 | `POST /api/v1/manager/orders/{code}/refunds` | Manager | cancellation/failure refund transaction |
| UC-34 | `POST /api/v1/manager/returns/{returnCode}/refunds` | Manager | received-return refund and completion |
| UC-35 | `GET /api/v1/manager/reports/cash` | Manager | cash movement aggregates |
| UC-36 | `GET /api/v1/manager/reports/sales` | Manager | completed-sales aggregates |
| UC-37 | `GET /api/v1/manager/reports/inventory` | Manager | inventory roll-forward |

The final RDS adds exact request/response field contracts. No generic `PATCH status` endpoint
is permitted for order/payment/return lifecycles.

## 8. Screen catalogue and authorization

| ID | React route/workspace | Main UCs | Guest | Customer | Staff | Manager |
| --- | --- | --- | :---: | :---: | :---: | :---: |
| SCR-01 | `/` — Catalogue/Home | UC-09 | X | X | X | X |
| SCR-02 | `/products/:productId` — Product Detail | UC-10, UC-11 | X | X | X | X |
| SCR-03 | `/cart` — Cart | UC-11 | X | X |  |  |
| SCR-04 | `/register`, `/verify-email` — Register/Verify | UC-01 | X |  |  |  |
| SCR-05 | `/login` — Login | UC-02 | X |  |  |  |
| SCR-06 | `/forgot-password`, `/reset-password` — Recovery | UC-07 | X |  |  |  |
| SCR-07 | `/account` — Profile/Security tabs by role | UC-04, UC-06 |  | X | X* | X* |
| SCR-08 | `/account/addresses` — Delivery Addresses | UC-05 |  | X |  |  |
| SCR-09 | `/checkout`, `/payment/result` — Checkout/Result | UC-12, UC-13 |  | X |  |  |
| SCR-10 | `/account/orders` — My Orders | UC-14 |  | X |  |  |
| SCR-11 | `/account/orders/:orderCode` — Detail/Receipt | UC-15, UC-16, UC-30 |  | X |  |  |
| SCR-12 | `/manager/staff` — Staff Accounts | UC-08 |  |  |  | X |
| SCR-13 | `/manager/catalog/classifications` — Category/Brand | UC-19 |  |  |  | X |
| SCR-14 | `/manager/catalog/products`, `/manager/catalog/products/:productId` — Product/SKU | UC-20 |  |  |  | X |
| SCR-15 | `/staff/orders` — Sales Order Search/Fulfillment Preset | UC-17 |  |  | X | X |
| SCR-16 | `/staff/orders/:orderCode` — Operational Detail | UC-18, UC-25–28, UC-34 |  |  | X** | X |
| SCR-17 | `/staff/inventory` — Overview/Adjustment | UC-21, UC-23 |  |  | X*** | X |
| SCR-18 | `/staff/stock-receipts/new` — Stock Receipt | UC-22 |  |  | X | X |
| SCR-19 | `/manager/inventory/movements` — Movement History | UC-24 |  |  |  | X |
| SCR-20 | `/staff/counter-sales/new` — Counter Sale/Receipt | UC-29 |  |  | X | X |
| SCR-21 | `/staff/returns/new` — In-Store Return Request | UC-31 |  |  | X | X |
| SCR-22 | `/staff/returns`, `/staff/returns/:returnCode` — Queue/Detail | UC-32–34 |  |  | X**** | X |
| SCR-23 | `/manager/reports/cash` — Cash Movement | UC-35 |  |  |  | X |
| SCR-24 | `/manager/reports/sales` — Sales Performance | UC-36 |  |  |  | X |
| SCR-25 | `/manager/reports/inventory` — Roll-Forward | UC-37 |  |  |  | X |

`*` Staff/Manager may change only their own password; no shared profile edit is promised.
`**` Staff cannot execute Manager cancellation/refund actions. `***` Staff view only; the
adjustment control is Manager-only. `****` Staff view/receive; Manager additionally decides
and refunds. Backend authorization remains authoritative even if controls are hidden.

Supporting/non-routed surfaces are recorded separately so they are not padded into screens:

| ID | Owning UCs | Supporting behavior |
| --- | --- | --- |
| SUP-01 | UC-02–03, UC-06 | global current-user/navigation/logout controls and CSRF refresh |
| SUP-02 | UC-01, UC-07–08 | SMTP token dispatch/failure and replacement-token behavior |
| SUP-03 | UC-13 | provider redirect/return/IPN verification plus pending-order expiry job |
| SUP-04 | UC-15, UC-18, UC-29 | browser print styling of the authorized receipt projection; no PDF/invoice table |
| SUP-05 | UC-12, UC-17–18, UC-25–34 | business-code/request-key generation and derived allowed actions/money states |
| SUP-06 | UC-02, UC-08 | idempotent first-Manager bootstrap from environment values; no manual database edit |

Each screen specification in RDS records loading, empty, validation, forbidden, conflict,
external-failure, and success states. Responsive support targets common laptop width and a
usable mobile customer catalogue/order view; internal dense tables may use horizontal scroll.

## 9. Database design conventions

- Schema: unquoted lowercase `app`; table/column/index names are snake_case.
- Primary keys: `bigint generated by default as identity` for this single database.
- Public business references: stable unique text codes; do not expose primary keys as order
  or return references.
- Time: `timestamptz`; no timezone-less event timestamp.
- Money: `numeric(14,0)`; no `real`, `double precision`, or text money.
- Quantity: `integer` with explicit checks.
- Status: text persisted from Java enum plus database CHECK constraint.
- Mutable aggregate rows carry `version bigint not null default 0` for JPA optimistic checks.
- Foreign keys use restrictive history-preserving delete behavior unless a dependent token
  can safely follow an unreferenced parent purge.
- Every FK is indexed unless an existing composite index starts with that FK.
- Email/name normalization occurs in service code and is reinforced with unique indexes such
  as `lower(email)` where useful.
- No JSONB is used for ordinary relational fields or audit shortcuts.
- Product sale units are represented only by `product_sku`; do not add generic
  attribute/value/option or generated-combination tables.

## 10. Planned table contracts

Common audit columns are `created_at` and `updated_at` where a row is mutable. Immutable
ledgers use only their event timestamp. Exact lengths/validation are finalized in RDS forms
and Flyway, not inferred from UI widgets.

| Table | Essential columns | Core constraints and indexes |
| --- | --- | --- |
| `user_account` | `user_id`, `email`, optional-until-setup `password_hash`, `full_name`, `phone`, `role`, `status`, `email_verified_at`, `version` | PK; unique lower email; role/status and ACTIVE-credential CHECK; index `(role,status)` |
| `account_token` | `token_id`, `user_id`, `token_type`, `token_hash`, `expires_at`, `used_at`, `created_at` | unique hash; type CHECK; indexes `(user_id,token_type)` and expiry |
| `customer_address` | `address_id`, `customer_id`, recipient/phone/address fields, `is_default`, `is_active`, `version` | FK; partial unique active default per customer; index customer/active |
| `category` | `category_id`, `name`, `is_active`, `version` | unique lower name; index active/name |
| `brand` | `brand_id`, `name`, `is_active`, `version` | unique lower name; index active/name |
| `product` | `product_id`, Category/Brand FKs, `name`, `description`, `image_url`, manufacturer/warranty text, `is_active`, `version` | FK indexes; index `(is_active,name,product_id)` and category/brand filters |
| `product_sku` | `sku_id`, `product_id`, `sku_code`, `sale_unit_label`, `list_price`, `reorder_level`, `on_hand_quantity`, `reserved_quantity`, `is_active`, `version` | new balance defaults zero; unique immutable SKU code; stock/price/threshold CHECKs; product FK index; active/stock index |
| `sales_order` | `order_id`, `order_code`, `request_key`, `request_fingerprint`, `channel`, `customer_id`, `status`, `payment_method`, `payment_expires_at`, delivery snapshots, money totals, lifecycle times/reasons, `created_by`, `version` | unique order code and actor/channel request scope; channel/status/payment/money CHECKs; customer/time and status/time indexes |
| `sales_order_item` | `order_item_id`, `order_id`, `sku_id`, SKU/sale-unit/Product/Category and price snapshots, `quantity`, `line_amount` | positive CHECKs; unique `(order_id,sku_id)`; both FK indexes; snapshot text is immutable |
| `order_status_history` | `history_id`, `order_id`, `from_status`, `to_status`, `source_type`, optional `changed_by`, `reason`, `reference`, `occurred_at` | immutable; source/status CHECKs; index `(order_id,occurred_at,history_id)` |
| `payment_attempt` | `attempt_id`, `order_id`, `attempt_code`, `provider`, provider refs, `expected_amount`, `currency`, `status`, `expires_at`, times | unique attempt code/provider transaction ref; one open attempt per order via partial unique index; status/amount CHECKs; order/status index |
| `payment_transaction` | `transaction_id`, `transaction_code`, `order_id`, optional `return_request_id`/`attempt_id`, `transaction_type`, `method`, `amount`, `source_type`, provider/manual reference, optional `recorded_by`, `recorded_at` | unique transaction code; positive amount; source/type/method CHECKs; unique provider ref where present; report/order indexes |
| `inventory_movement` | `movement_id`, `sku_id`, optional order/receipt/return FKs, `movement_type`, both deltas/after-balances, `reason`, `source_type`, optional `actor_id`, `occurred_at` | non-zero delta and balance/source-reference CHECKs; SKU/time and business-reference indexes |
| `stock_receipt` | `receipt_id`, `receipt_code`, `request_key`, `request_fingerprint`, `source_name`, `source_reference`, `note`, `received_by`, `received_at` | unique receipt code and operator/request scope; received time index |
| `stock_receipt_item` | `receipt_item_id`, `receipt_id`, `sku_id`, `quantity` | positive quantity; unique `(receipt_id,sku_id)`; SKU FK index |
| `return_request` | `return_id`, `return_code`, `order_id`, `request_key`, `request_fingerprint`, `request_channel`, `reason`, `status`, `requested_by/created_at`, optional decision actor/time/note, optional intake actor/time/note, `version` | unique return code/order and actor/channel request scope; reason/status/timestamp CHECKs; queue index `(status,created_at,return_id)` |
| `return_item` | `return_item_id`, `return_id`, `order_item_id`, requested/received/sellable/non-sellable quantities | unique `(return_id,order_item_id)`; before intake disposition defaults zero; quantity/disposition CHECKs; both FK indexes |

### Required database checks

At minimum Flyway must enforce:

- all money totals are nonnegative and order total arithmetic is consistent;
- ACTIVE account requires non-null password hash and verification/setup timestamp;
- order items/receipt items/requested return quantities are positive;
- line/header command bounds are rechecked in services; per-row sale quantity is at most 999
  and receipt quantity at most 999,999;
- `0 <= reserved_quantity <= on_hand_quantity` and reorder level is nonnegative;
- inventory movement has at least one non-zero delta and nonnegative after-balances;
- movement type requires its owning order/receipt/return reference; `OPENING_BALANCE` and
  `ADJUSTMENT` require an explicit reason/source instead of a fabricated business FK;
- return disposition quantities are nonnegative and sum exactly to received quantity;
- received return quantity does not exceed requested quantity at row level;
- COUNTER order has CASH, no delivery address/fee, and terminal completed state;
- DELIVERY order has Customer/delivery snapshot and COD or ONLINE method;
- CUSTOMER return request targets an owned DELIVERY order; IN_STORE may target either channel;
- lifecycle timestamps are present only/at least where the final state requires them;
- transaction amount is positive and transaction type/method combination is allowed.
- USER source rows require an actor; SYSTEM/PROVIDER rows require a non-secret reference and
  may have no human actor.

Exact source values are `USER`, `SYSTEM`, and `PROVIDER`. Payment transaction types are
`COLLECTION` and `REFUND`. Allowed transaction methods are `ONLINE`, `COD`, `CASH`, and
`BANK_TRANSFER`, constrained by type and originating order/return context. These ledger
vocabularies are not interchangeable with order channel or return-request channel.

Cross-row totals, lifecycle legality, one-use token consumption, refund caps, and order-item
ownership are additionally enforced in locked service transactions because CHECK constraints
cannot safely express them across rows.

### Delete and retention policy

| Data class | Policy |
| --- | --- |
| Accounts/catalogue/address | deactivate; do not delete when referenced |
| Orders/items/histories | never delete through application |
| Payments/movements/receipts/returns | immutable or state-guarded; never destructive-delete |
| Expired/used account tokens | purgeable by maintenance after a documented retention period |
| Demo/test data | recreated only in disposable environment via seed script/migration location |

## 11. Initial index plan

Indexes are justified by traced queries, not added to every column. Baseline candidates:

| Query path | Candidate index |
| --- | --- |
| Login/account lookup | unique `lower(user_account.email)` |
| Staff account list | `(role, status, user_id)` |
| Customer address list/default | `(customer_id, is_active, address_id)` plus partial unique default |
| Public product catalogue | `(is_active, name, product_id)`; Category/Brand FK indexes |
| SKU by Product/code | `(product_id, is_active, sku_id)` and unique `sku_code` |
| Customer order history | `(customer_id, created_at desc, order_id desc)` |
| Staff order queue/search | `(status, created_at, order_id)` and `(channel, created_at desc, order_id desc)` |
| Pending-payment expiry job | partial `(payment_expires_at, order_id)` where status = `PENDING_PAYMENT` |
| Idempotent order creation | partial unique `(customer_id, request_key)` for DELIVERY and `(created_by, request_key)` for COUNTER |
| Order history | `(order_id, occurred_at, history_id)` |
| Payment processing | `(order_id, status, attempt_id)` and unique provider transaction ref |
| One open payment attempt | partial unique `order_id` where status in (`CREATED`,`PENDING`) |
| Cash report | `(recorded_at, method, transaction_type)` |
| Inventory overview/low stock | SKU/product active-state indexes; inactive filter remains available; test expression/partial index only if query plan needs it |
| Movement history/report | `(sku_id, occurred_at, movement_id)` and `(occurred_at, movement_type)` |
| Return queue | `(status, created_at, return_id)` and unique `order_id` |
| Receipt/return retry | unique `(received_by, request_key)` on receipt; unique `(requested_by, request_key)` on return |

Use `EXPLAIN (ANALYZE, BUFFERS)` only on seeded non-production-like data when validating
important queries. Avoid redundant single indexes already covered by a useful composite left
prefix. Ordinary offset paging is accepted for the small classroom dataset with deterministic
sorts; keyset pagination is future work.

## 12. Flyway and Supabase contract

### Schema ownership

- Flyway migrations create/use `app`; JPA default schema is `app`.
- Hibernate runs `ddl-auto=validate` in local, CI, and deployed profiles.
- Never edit an applied migration. Add a new migration and document the data transition.
- A schema diff/check runs before SDS to prove migrations recreate the documented schema.

Suggested migration sequence, adjusted as implementation lands:

```text
V1__create_app_schema_and_identity.sql
V2__create_catalog_and_inventory_balance.sql
V3__create_orders_payments_and_histories.sql
V4__create_receipts_and_inventory_movements.sql
V5__create_returns.sql
V6__add_query_indexes_and_runtime_privileges.sql
```

Demo seeds live in an explicit dev/test Flyway location or versioned seed script and never run
silently in production. Reference/status values come from application enums and CHECKs; avoid
lookup tables with no lifecycle or behavior.

### Connection choice

- Persistent Spring backend uses Supabase direct JDBC when the host supports IPv6, otherwise
  Supavisor session mode on port 5432.
- Do not use transaction mode 6543 as the normal Hibernate datasource because prepared
  statements/session behavior can conflict.
- Require SSL. Credentials are environment variables, never committed.
- Start Hikari with a deliberately small pool, recommended maximum 5 and minimum idle 1 for
  the student deployment; adjust only from measured connection limits.
- Use a separate migration credential and least-privilege runtime role where the chosen host
  setup supports it. CI ephemeral PostgreSQL may use one disposable owner role.

### RLS decision

The baseline does not use Supabase end-user Auth/Data API. Tables live in a backend-only
schema and the runtime JDBC role is not exposed to browsers. Spring services enforce user
ownership. Do not add RLS policies that depend on `auth.uid()` when no Supabase user identity
exists. If the team later exposes a schema through Supabase APIs, that is an architecture
change and RLS becomes a new mandatory security design, not a toggle.

## 13. Query contracts

| Query ID | Owner UC | Required filters/semantics | Result guard |
| --- | --- | --- | --- |
| QRY-01 | UC-09 | active Product; search; Category/Brand; deterministic sort/paging | no inactive leakage; no entity graph/N+1 |
| QRY-02 | UC-10 | active Product plus active SKU projection | missing/inactive returns 404 |
| QRY-03 | UC-14 | authenticated customer ID plus optional status | owner predicate is mandatory |
| QRY-04 | UC-15 | order code plus authenticated customer ID | no “load then check in React” |
| QRY-05 | UC-17 | status/channel/money-state/code/customer/date filters; optional actionable preset for `CONFIRMED`, `PACKING`, `SHIPPING` | allow-list filters/sort; actionable status priority then age; bounded page |
| QRY-06 | UC-18 | order code with items/history/payment/return projections | avoid Cartesian duplicate graph |
| QRY-07 | UC-21 | current SKU balances, active-state filter, and low-stock predicate | `available=on_hand-reserved`; inactive stock remains discoverable |
| QRY-08 | UC-24 | SKU/order/reference/time filters | immutable rows; time desc + ID desc |
| QRY-09 | UC-32–34 | return status/created filters and detail | role controls available commands |
| QRY-10 | UC-35 | payment transaction half-open range; group date/method/type | collected-refunded=net |
| QRY-11 | UC-36 | completed_at half-open range; sale snapshot aggregates | never relabel as cash |
| QRY-12 | UC-37 | movements before/start/end by SKU | opening+deltas=closing |

RDS defines fields, default filters, empty state, sort, date meaning, and totals for each.
SDS records the actual repository method/JPQL/SQL and index used.

Each report returns one coherent database snapshot. Prefer one SQL/JPQL projection; if summary
and detail require several statements, execute them in one read-only `REPEATABLE READ`
transaction. Never combine totals fetched at different moments in React.

## 14. Testing contract

### Test layers

| Layer | Purpose | Required examples |
| --- | --- | --- |
| Pure unit | price/fee/refund/date/status policies | boundaries, invalid transitions, fixed Clock |
| Service + PostgreSQL | transaction, locks, constraints, ledgers | checkout races, rollback, refund cap, movement reconciliation |
| MockMvc/controller | JSON validation, status code, role/ownership, CSRF | 400/401/403/404/409 and safe body |
| Repository | filters, projection, paging, report totals | deterministic sorts, no duplicate rows, half-open dates |
| React component | forms, role navigation, error/loading/empty states | cart, checkout conflict, protected control hiding |
| Browser smoke | integrated user paths | COD, online payment sandbox, counter sale, return/refund |

### Mandatory concurrency/idempotency cases

1. Two Customers buy the last unit: exactly one succeeds.
2. Checkout request key retried: one order and one reservation set.
3. Counter-sale request key retried: one order/collection/stock decrease.
4. Cancel races with payment success: one legal terminal state, one collection at most,
   reservation correct, refund due if required.
5. Duplicate provider result: no duplicate collection/history.
6. Dispatch submitted twice: one stock decrease.
7. Delivery outcome submitted twice: one terminal transition and one COD collection/return.
8. Stock adjustment with stale version: second request receives 409 and no second movement.
9. Return creation submitted twice: one request.
10. Refund submitted twice: refund cap prevents duplicate money row.

### Reconciliation assertions

- For every SKU fixture, current balance equals opening seed plus movement deltas.
- For every order, merchandise total equals item line sum and total arithmetic holds.
- Collected minus refunded equals derived net transaction value.
- Inventory report opening plus categorized deltas equals closing.
- Report date boundary tests include exactly midnight start and exclude next-day midnight.

Tests must create data through services/fixtures that preserve constraints. Manual Supabase
Table Editor edits are not acceptance evidence.

## 15. GitLab workflow and CI

### Repository rules

- Protect `main`; no direct push and no force push.
- Branch name contains the work item, for example `feature/UC-12-checkout` or
  `fix/UC-26-dispatch-lock`.
- Every merge uses a Merge Request, green pipeline, linked issue, and at least one peer review.
- The author cannot be the only person who can explain the merged high-risk behavior.
- Secrets use masked/protected CI variables. Sample `.env.example` contains names only.

### Issue template fields

```text
UC / BR / NFR IDs
Actor and observable result
In-scope normal and alternative flows
React route/screen and API changes
Service transaction/lock/idempotency effects
Tables/queries/migrations
Acceptance tests and demo data
RDS/traceability updates
Owner and reviewer
Out-of-scope notes
```

### Merge Request checklist

```text
[ ] Linked UC/BR issue and no unapproved scope
[ ] Authorization/ownership/CSRF covered
[ ] Migration is additive and PostgreSQL-tested
[ ] Transaction/state/stock/money effects tested
[ ] React loading/empty/error/success states handled
[ ] No secret, entity serialization, or direct Supabase browser call
[ ] RDS and traceability updated for behavior change
[ ] Demo path/reviewer explanation recorded
```

### Pipeline stages

| Stage | Minimum jobs | Failure meaning |
| --- | --- | --- |
| `validate` | planning ID/link checks, formatting/lint | artifact inconsistency or basic quality failure |
| `frontend-test` | `npm ci`, lint, Vitest | React contract/regression failure |
| `backend-test` | Maven unit/controller/integration tests with PostgreSQL service | domain/security/schema failure |
| `build` | frontend production build then Spring package | deployable artifact cannot be reproduced |
| `smoke` | start packaged app against disposable PostgreSQL, health/login/catalogue smoke | packaging/config/migration integration failure |

Use dependency caches for speed and artifacts to pass the frontend/build output; caches are
not trusted as release artifacts. Tag the final SDS evidence commit.

## 16. Non-functional requirements

| ID | Requirement | Verifiable acceptance |
| --- | --- | --- |
| NFR-01 | Architecture | One Spring Boot deployable serves React and `/api/v1`; no browser Supabase connection |
| NFR-02 | Correctness | Stock/money/order/return invariants and report reconciliations pass automated tests |
| NFR-03 | Authentication | Protected API requires active session; logout invalidates it |
| NFR-04 | Authorization/privacy | Server rejects wrong role/owner; DTOs contain no secret/internal fields |
| NFR-05 | CSRF/session safety | Unsafe session-authenticated requests require token; token refresh works after login/logout |
| NFR-06 | Database reproducibility | Flyway creates empty `app` schema; Hibernate validate passes; no manual DDL required |
| NFR-07 | Transaction safety | High-risk commands are atomic, idempotent/state-guarded, and deadlock order is stable |
| NFR-08 | Query behavior | Page size is bounded, stable sort exists, FK/filter indexes reviewed, no obvious N+1 on main lists |
| NFR-09 | External failure | SMTP/payment outage yields safe observable state and retry path, never false success |
| NFR-10 | Time/money | Whole VND and injected Clock; all date reports use documented half-open shop-time ranges |
| NFR-11 | Usability | Vietnamese loading/empty/error/success states; core customer flow works on mobile and laptop |
| NFR-12 | Accessibility | Keyboard-reachable forms/actions, associated labels, visible focus, meaningful validation text |
| NFR-13 | Logging | Correlation ID and business code/status logged; no password/token/secret/full sensitive payload |
| NFR-14 | CI quality | Every MR runs frontend/backend/PostgreSQL/build checks; `main` remains green/protected |
| NFR-15 | Maintainability | Feature boundaries, DTOs, migrations, and trace IDs make each UC defensible by its owner |
| NFR-16 | Documentation | RDS/traceability update accompanies behavior change; SDS references final commit and real symbols |

Do not invent a numeric response-time SLA unsupported by course infrastructure. For seeded
demo data, representative catalogue/order/report screens must load without visible repeated
query storms, unbounded results, or connection exhaustion.

## 17. Deployment and configuration

Required environment-variable names are documented without values, for example:

```text
SPRING_PROFILES_ACTIVE
DB_URL
DB_USERNAME
DB_PASSWORD
SESSION_COOKIE_SECURE
APP_BASE_URL
SMTP_HOST
SMTP_PORT
SMTP_USERNAME
SMTP_PASSWORD
PAYMENT_TMN_CODE
PAYMENT_HASH_SECRET
PAYMENT_BASE_URL
APP_DELIVERY_FEE_VND
APP_PAYMENT_EXPIRY_MINUTES
SERVER_SERVLET_SESSION_TIMEOUT
APP_STORE_NAME
APP_STORE_ADDRESS
APP_STORE_PHONE
BOOTSTRAP_MANAGER_EMAIL
BOOTSTRAP_MANAGER_PASSWORD
```

The actual provider may require different names. One configuration class validates required
values at startup for the active profile. Local/test profiles use safe disposable credentials.
The release guide must prove fresh clone -> install -> migrate -> seed -> test -> package -> run.

## 18. RDS and SDS evidence hooks

For each UC, preserve these links while implementing:

```text
UC/BR/NFR
-> SCR route and API endpoint
-> request/response/error DTO
-> service command/query and transaction/lock policy
-> table/constraint/index/query ID
-> test class/case and demo fixture
-> GitLab issue/MR/commit
-> RDS section/screenshot
-> SDS class/component/sequence/query evidence
```

The initial mapping is in `11_TRACEABILITY_SEED.md`. Actual class/method/test/commit fields
stay blank until implementation exists; placeholders must never be presented as SDS facts.

## 19. Primary technical references

- [Supabase database connection choices](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Supabase Spring Boot connection guidance](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot)
- [Spring Security CSRF and SPA guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [React build-from-scratch guidance including Vite](https://react.dev/learn/build-a-react-app-from-scratch)
- [GitLab protected branches](https://docs.gitlab.com/user/project/repository/branches/protected/)
- [GitLab CI/CD YAML reference](https://docs.gitlab.com/ci/yaml/)
- [GitLab CI cache and artifact distinction](https://docs.gitlab.com/ci/caching/)
- [VNPAY sandbox payment flow](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html)
