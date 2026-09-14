# Requirements-to-RDS-to-SDS Traceability Seed

Status: **complete planned coverage for UC-01 through UC-37; implementation evidence blank by design**

## 1. Purpose

This matrix prevents the most expensive SWP391 failure mode: the use-case diagram, detailed
flows, screens, code, database, tests, backlog, and SDS each describing a slightly different
system. It maps every committed actor goal to the rules and planned design evidence already
approved in this folder.

This is a seed, not fabricated SDS evidence. Route/table/query entries are planned contracts.
Actual package, class, method, migration, test result, MR, screenshot, and sequence-diagram
references remain blank until they exist in the tagged implementation.

Authority order is:

```text
DEC -> Scope -> UC -> BR/state -> SCR/API/data/query/NFR -> RDS
    -> GitLab issue/MR -> verified implementation/test -> SDS
```

If two cells disagree, repair the earliest authoritative source and propagate the change.

## 2. Test-ID convention

Every UC receives at least `AT-UC-xx-N` for its normal flow and `AT-UC-xx-A` for its most
important alternative/error flow. The compact matrix notation `AT-UC-xx-{N,A}` means both
tests; add numbered suffixes when there are several. Shared high-risk suites are:

| Test ID | Cross-cutting oracle |
| --- | --- |
| SEC-01 | session creation/rotation/invalidation plus fresh CSRF after login/logout |
| SEC-02 | full route-role matrix and hidden-control tampering |
| SEC-03 | Customer ownership/isolation for profile, address, order, payment, return |
| IDEM-01 | same key+fingerprint returns original; same key+different fingerprint is 409 |
| CON-01 | two checkouts for last stock unit: exactly one commits |
| PAY-01 | provider signature, merchant, currency, reference, exact amount, result code |
| PAY-02 | duplicate/failed/expired/late provider results create at most one collection |
| RACE-01 | cancellation or expiry racing payment success leaves one legal order/money result |
| STK-01 | each command's on-hand/reserved deltas equal immutable movement rows |
| STK-02 | independent opening + movement deltas equal every current SKU balance |
| FUL-01 | transition allow-list and duplicate pack/dispatch/outcome guards |
| RET-01 | deadline, ownership/evidence, one-request, reason, and item quantity bounds |
| RET-02 | approve-all/reject and one positive intake with disposition equation |
| REF-01 | exact remaining refund cap, duplicate guard, and atomic return completion |
| REP-01 | cash report uses transaction time and collection - refund = net |
| REP-02 | gross sales report uses completion time and item snapshots |
| REP-03 | inventory opening + categorized deltas = closing using movement time |
| TIME-01 | fixed Clock and half-open boundary tests, including next-day exclusion |
| REL-01 | clean clone can migrate, seed, test, package, start, and execute smoke paths |

## 3. Baseline UC traceability matrix

API paths are abbreviated only where `08_TECHNICAL_FOUNDATION_BLUEPRINT.md` already owns the
full route contract. `Command` means no named QRY ID is needed; repository locks/lookups still
must be documented in SDS.

| UC / actor goal | Governing BRs | Screen / API | Planned tables or query | Core planned tests | Lead / reviewer |
| --- | --- | --- | --- | --- | --- |
| UC-01 Register Customer Account | BR-03–04, BR-58, BR-62–64 | SCR-04; `POST /auth/register`, `/verify-email`, `/resend-verification` | `user_account`, `account_token`; Command | AT-UC-01-{N,A}, SEC-01, TIME-01 | A / B |
| UC-02 Login | BR-01–04, BR-62–63 | SCR-05; `POST /auth/login`; SUPPORT `/auth/me`, `/auth/csrf` | `user_account`; Command | AT-UC-02-{N,A}, SEC-01–02 | A / C |
| UC-03 Logout | BR-02–04, BR-62–63 | global account menu; `POST /auth/logout` | session state; no business table/query | AT-UC-03-{N,A}, SEC-01 | A / C |
| UC-04 Manage Personal Profile | BR-02, BR-06, BR-58, BR-61–63 | SCR-07; `GET/PUT /me/profile` | `user_account`; Command | AT-UC-04-{N,A}, SEC-03 | A / B |
| UC-05 Manage Delivery Addresses | BR-02, BR-06, BR-13, BR-58, BR-61–63 | SCR-08; `/me/addresses/**` | `customer_address`, `user_account`; Command | AT-UC-05-{N,A}, SEC-03 | A / C |
| UC-06 Change Own Password | BR-02, BR-04, BR-62–63 | SCR-07; `PUT /account/password` | `user_account` plus session registry; Command | AT-UC-06-{N,A}, SEC-01 | A / C |
| UC-07 Reset Forgotten Password | BR-03–04, BR-58, BR-62–63 | SCR-06; `/auth/password-reset/**` | `user_account`, `account_token`; Command | AT-UC-07-{N,A}, SEC-01, TIME-01 | A / B |
| UC-08 Manage Staff Accounts | BR-01–05, BR-58, BR-61–63 | SCR-12; `/manager/staff-accounts/**` | `user_account`, `account_token`; Command | AT-UC-08-{N,A}, SEC-01–02 | A / E |
| UC-09 Browse and Search Products | BR-07–11, BR-57–58, BR-63 | SCR-01; `GET /catalog/products` | `category`, `brand`, `product`, `product_sku`; QRY-01 | AT-UC-09-{N,A}, TIME-01 paging boundary | B / D |
| UC-10 View Product Detail | BR-07–11, BR-57, BR-63 | SCR-02; `GET /catalog/products/{productId}` | `category`, `brand`, `product`, `product_sku`; QRY-02 | AT-UC-10-{N,A} | B / D |
| UC-11 Manage Shopping Cart | BR-08–12, BR-63 | SCR-02/03; browser state, QRY-01/02 reads only | no cart table; browser local storage | AT-UC-11-{N,A}, corrupt/stale storage test | B / A |
| UC-12 Place Customer Order | BR-09–19, BR-22, BR-25–27, BR-31, BR-58–64 | SCR-09; `POST /orders` | `customer_address`, `product_sku`, `sales_order`, `sales_order_item`, `order_status_history`, `inventory_movement`; Command | AT-UC-12-{N,A}, IDEM-01, CON-01, STK-01 | C / D |
| UC-13 Complete Online Payment | BR-02, BR-17, BR-22, BR-25–27, BR-32–38, BR-58–64 | SCR-09; `/me/orders/{code}/payment-attempts`, signed provider return/IPN | `sales_order`, `order_status_history`, `payment_attempt`, `payment_transaction`, `product_sku`, `inventory_movement`; Command | AT-UC-13-{N,A}, PAY-01–02, RACE-01, STK-01, TIME-01 | C / A |
| UC-14 View My Order History | BR-06, BR-25, BR-31, BR-57–58, BR-62–63 | SCR-10; `GET /me/orders` | `sales_order`; QRY-03 | AT-UC-14-{N,A}, SEC-03 | A / B |
| UC-15 View Own Order Detail and Receipt | BR-02, BR-06, BR-25, BR-30–32, BR-36–38, BR-44, BR-47, BR-57–58, BR-60, BR-62–64 | SCR-11; `GET /me/orders/{code}` | order/items/history, attempts/transactions, return/items; QRY-04 | AT-UC-15-{N,A}, SEC-03, receipt eligibility test | B / A |
| UC-16 Cancel Eligible Own Order | BR-06, BR-22, BR-25–29, BR-31, BR-35–38, BR-58–63 | SCR-11; `POST /me/orders/{code}/cancellation` | `sales_order`, `order_status_history`, `payment_attempt`, `payment_transaction`, `product_sku`, `inventory_movement`; Command | AT-UC-16-{N,A}, SEC-03, RACE-01, STK-01 | C / D |
| UC-17 View and Search Sales Orders | BR-01–02, BR-25–27, BR-31, BR-57–58, BR-60, BR-62–64 | SCR-15; `GET /staff/orders` | `sales_order`; QRY-05 including actionable fulfillment preset | AT-UC-17-{N,A}, SEC-02, FUL-01, TIME-01 | E / B |
| UC-18 View Sales Order Detail | BR-01–02, BR-06, BR-25, BR-30–32, BR-36–38, BR-44, BR-47, BR-57–58, BR-60, BR-62–64 | SCR-16; `GET /staff/orders/{code}` | order/items/history, attempts/transactions, return/items; QRY-06 | AT-UC-18-{N,A}, SEC-02, projection-duplication test | E / C |
| UC-19 Manage Product Classification Data | BR-01–02, BR-07–08, BR-61–63 | SCR-13; `/manager/categories`, `/manager/brands` | `category`, `brand`, `product`; Command | AT-UC-19-{N,A}, SEC-02, normalized-unique test | B / D |
| UC-20 Manage Product Catalogue and SKUs | BR-01–02, BR-07–11, BR-18, BR-61–63 | SCR-14; `/manager/products/**`, nested `/skus` | `category`, `brand`, `product`, `product_sku`; Command | AT-UC-20-{N,A}, SEC-02, activation/version tests | B / D |
| UC-21 View Inventory Overview | BR-08, BR-18, BR-57–58, BR-63 | SCR-17; `GET /staff/inventory` | `product`, `product_sku`; QRY-07 | AT-UC-21-{N,A}, STK-02 | D / B |
| UC-22 Receive Stock | BR-09, BR-18–20, BR-58–61, BR-63 | SCR-18; `POST /staff/stock-receipts` | `product_sku`, `stock_receipt`, `stock_receipt_item`, `inventory_movement`; Command | AT-UC-22-{N,A}, IDEM-01, STK-01–02 | D / C |
| UC-23 Adjust Stock | BR-09, BR-18–21, BR-58–61, BR-63 | SCR-17; `POST /manager/inventory/{skuId}/adjustments` | `product_sku`, `inventory_movement`; Command | AT-UC-23-{N,A}, SEC-02, stale-version test, STK-01–02 | D / C |
| UC-24 View Stock Movement History | BR-19, BR-57–58, BR-61–63 | SCR-19; `GET /manager/inventory-movements` | `inventory_movement`, `product_sku`; QRY-08 | AT-UC-24-{N,A}, SEC-02, STK-02, TIME-01 | D / C |
| UC-25 Pack Confirmed Order | BR-25–27, BR-31, BR-39, BR-59, BR-62–63 | SCR-16; `POST /.../orders/{code}/pack` | `sales_order`, `order_status_history`; Command | AT-UC-25-{N,A}, SEC-02, FUL-01, zero-stock-delta test | E / D |
| UC-26 Dispatch Order for Delivery | BR-18–19, BR-22–23, BR-25–27, BR-31, BR-40, BR-59–63 | SCR-16; `POST .../orders/{code}/dispatch` | `sales_order`, `sales_order_item`, `order_status_history`, `product_sku`, `inventory_movement`; Command | AT-UC-26-{N,A}, FUL-01, STK-01, duplicate/race test | E / D |
| UC-27 Record Delivery Outcome | BR-18–19, BR-23–27, BR-31, BR-36–38, BR-41–42, BR-59–63 | SCR-16; `POST .../orders/{code}/delivery-outcome` | `sales_order`, order items/history, `product_sku`, `inventory_movement`, `payment_transaction`; Command | AT-UC-27-{N,A}, FUL-01, STK-01, COD/duplicate atomicity | E / C |
| UC-28 Cancel Order by Store | BR-18–19, BR-22, BR-25–29, BR-31, BR-35–38, BR-59–63 | SCR-16; `POST /manager/orders/{code}/cancellation` | `sales_order`, history, attempts/transactions, `product_sku`, `inventory_movement`; Command | AT-UC-28-{N,A}, SEC-02, RACE-01, STK-01 | C / E |
| UC-29 Sell Products at Counter | BR-07–11, BR-16–19, BR-23, BR-25, BR-27, BR-36–37, BR-43, BR-58–63 | SCR-20; `POST /staff/counter-sales` | `product_sku`, `sales_order`, items/history, `payment_transaction`, `inventory_movement`; Command | AT-UC-29-{N,A}, IDEM-01, CON-01 equivalent, STK-01 | D / C |
| UC-30 Request Return for Own Order | BR-06, BR-30–31, BR-44–47, BR-58–63 | SCR-11; `POST /me/orders/{code}/returns` | `sales_order`, `sales_order_item`, `return_request`, `return_item`; Command | AT-UC-30-{N,A}, SEC-03, IDEM-01, RET-01, TIME-01 | B / E |
| UC-31 Create In-Store Return Request | BR-30–31, BR-43–47, BR-58–63 | SCR-21; `POST /staff/returns/in-store` | `sales_order`, `sales_order_item`, `return_request`, `return_item`; Command | AT-UC-31-{N,A}, SEC-02, IDEM-01, RET-01 | D / E |
| UC-32 Review Return Request | BR-47–48, BR-59, BR-62–63 | SCR-22; `POST /manager/returns/{code}/decision`; SUPPORT return query | `return_request`, `return_item`; QRY-09 + Command | AT-UC-32-{N,A}, SEC-02, RET-02, stale-version test | E / B |
| UC-33 Receive and Inspect Returned Items | BR-18–19, BR-24, BR-47, BR-49–50, BR-58–63 | SCR-22; `POST /staff/returns/{code}/receipt`; SUPPORT return query | `sales_order`, `return_request`, `return_item`, `product_sku`, `inventory_movement`; QRY-09 + Command | AT-UC-33-{N,A}, RET-02, STK-01–02, duplicate test | E / D |
| UC-34 Record Refund Completion | BR-30, BR-35–38, BR-47, BR-50–53, BR-58–63 | SCR-16/22; `POST /manager/orders/{code}/refunds`, `/manager/returns/{code}/refunds` | `sales_order`, `return_request`, `return_item`, `payment_transaction`; Command | AT-UC-34-{N,A}, SEC-02, REF-01, RACE-01 case | C / E |
| UC-35 View Cash Movement Report | BR-01–02, BR-11, BR-36, BR-54, BR-57–58, BR-60, BR-62–64 | SCR-23; `GET /manager/reports/cash` | `payment_transaction`; QRY-10 | AT-UC-35-{N,A}, SEC-02, REP-01, TIME-01 | C / B |
| UC-36 View Sales Performance Report | BR-01–02, BR-11, BR-16–17, BR-25, BR-55, BR-57–58, BR-60, BR-62–64 | SCR-24; `GET /manager/reports/sales` | `sales_order`, `sales_order_item`; QRY-11 | AT-UC-36-{N,A}, SEC-02, REP-02, TIME-01 | B / C |
| UC-37 View Inventory Roll-Forward Report | BR-01–02, BR-18–19, BR-56–58, BR-60–64 | SCR-25; `GET /manager/reports/inventory` | `inventory_movement`, `product_sku`; QRY-12 | AT-UC-37-{N,A}, SEC-02, REP-03, STK-02, TIME-01 | D / C |

All abbreviated API paths inherit `/api/v1`. The matrix may cite a BR range only when every
rule in that range genuinely applies; detailed RDS descriptions should list exact IDs if a
range would be misleading.

## 4. Planned table accountability

Every one of the 17 business tables has a creation/mutation owner and at least one read or
audit purpose. This table prevents speculative schema growth and ownerless data.

| Table | Created/mutated by | Materially read by | Reconciliation/retention oracle |
| --- | --- | --- | --- |
| `user_account` | UC-01, UC-04, UC-06–08 | UC-02, UC-04–08, authorization support | active/role/token/session rules; no hard delete when referenced |
| `account_token` | UC-01, UC-07–08 | same owning UCs | hash unique, one-use, exact expiry/replacement |
| `customer_address` | UC-05 | UC-05, UC-12, UC-15/18 snapshots | owner/default/active rules; order snapshots remain immutable |
| `category` | UC-19 | UC-09–10, UC-20 | normalized uniqueness; active-reference guard |
| `brand` | UC-19 | UC-09–10, UC-20 | normalized uniqueness; active-reference guard |
| `product` | UC-20 | UC-09–10, UC-20–21 | active classification/SKU rule; historical report names use item snapshots |
| `product_sku` | UC-20–23 and all stock-changing commands | catalogue, order, inventory, returns, reports | current balance equals movement ledger; no unexplained seed stock |
| `sales_order` | created by UC-12/29; transitioned by UC-13/16/25–28 | UC-13–18, UC-25–37 as applicable | legal state/channel/payment/totals; no application delete |
| `sales_order_item` | UC-12, UC-29 | order details, fulfillment, returns, sales report | unique SKU/order; immutable SKU/Product/Category/price snapshots; line sums equal merchandise |
| `order_status_history` | UC-12–13, UC-16, UC-25–29 | UC-15/18 | one row per committed creation/transition; no rejected-command row |
| `payment_attempt` | created/finalized by UC-13; expired by UC-13/16/28 | UC-13, UC-15–16, UC-18, UC-28 | one open attempt/order; provider result/idempotency/expiry rules |
| `payment_transaction` | UC-13, UC-27, UC-29, UC-34 | order detail, refund cap, cash report | positive immutable collection/refund; collected-refunded=net |
| `inventory_movement` | UC-12–13, UC-16, UC-22–23, UC-26–29, UC-33 | UC-21, UC-24, UC-37 | opening + deltas = current/closing; immutable |
| `stock_receipt` | UC-22 | UC-22, UC-24 | scoped key+fingerprint; no Supplier/PO implication |
| `stock_receipt_item` | UC-22 | UC-22, UC-24 | positive unique SKU lines; header/lines/balance atomic |
| `return_request` | UC-30–31; state by UC-32–34 | UC-15/18, UC-32–34 | unique order; legal single decision/intake/refund lifecycle |
| `return_item` | UC-30–31; disposition by UC-33 | UC-15/18, UC-32–34 | requested/received/sellable/non-sellable equations and caps |

Adding a table requires a new row naming its owning UC/BR/query/test and a capacity impact.
Cart, role, invoice, refund, dashboard, generic audit, supplier, promotion, and report tables
remain prohibited unless an approved scope change justifies them.

## 5. NFR coverage ownership

| NFR cluster | Primary proving artifacts | Gate |
| --- | --- | --- |
| NFR-01, NFR-06, NFR-14 | TECH-01/03/04, CI build/smoke, REL-01 | R1 and R5 |
| NFR-03–05 | SEC-01–03, MockMvc plus browser smoke | R1, then every protected UC |
| NFR-02, NFR-07, NFR-10 | CON/PAY/RACE/STK/FUL/RET/REF/REP/TIME suites | R2–R4 |
| NFR-08 | QRY-01–12 tests, stable paging, index review, bounded data | each query MR and R4 |
| NFR-09 | SMTP/provider outage and retry fixtures | R1 identity proof and R4 payment proof |
| NFR-11–12 | screen-state/accessibility checklist and browser demo | every screen MR and R5 |
| NFR-13 | correlation/redaction tests and log inspection | R1 and external-command MRs |
| NFR-15–16 | package boundary, MR review, this matrix, final RDS/SDS audit | every MR and R5/R6 |

## 6. Core demo-to-requirement map

| Demo ID | Integrated path | Primary UCs | Required adverse proof |
| --- | --- | --- | --- |
| DEMO-01 | Guest catalogue -> Customer login/cart/address -> COD order -> pack/dispatch/success -> receipt/reports | UC-02, UC-05, UC-09–12, UC-14–15, UC-17–18, UC-21, UC-25–27, UC-35–37 | stale cart and last-unit race; duplicate dispatch/outcome |
| DEMO-02 | DELIVERY order with ONLINE payment -> signed result -> fulfillment -> receipt | UC-12–18, UC-25–28, UC-34–36 | bad/duplicate result and cancellation/expiry/late-success race |
| DEMO-03 | stock receipt/adjustment -> anonymous COUNTER cash sale -> receipt | UC-20–24, UC-29, UC-35–37 | stale adjustment and duplicate/last-unit counter sale |
| DEMO-04 | completed sale -> Customer or in-store request -> decision -> one intake -> exact refund | UC-15, UC-18, UC-30–34 | wrong owner/window/quantity, second request/intake/refund |

Demo data must include at least one row for each order/payment/return state, one inactive SKU
with remaining stock, one out-of-stock SKU, one low-stock SKU, one rejected return, sellable
and non-sellable returned quantities, a late valid payment result, and events exactly on date
boundaries. Synthetic data must contain no real personal details or real credentials.

## 7. Living implementation evidence ledger

At implementation start, create one ledger row per UC with these columns. Do not fill a cell
with “TBD” and later present it as proof.

| Column | When it becomes factual |
| --- | --- |
| UC / BR / NFR / SCR / QRY IDs | RDS freeze |
| GitLab issue and milestone | feature accepted into `Ready` |
| React route/page/component/hook/API client | merged code inspected |
| Controller endpoint and DTO symbols | merged code inspected |
| Service method, transaction and lock policy | merged code plus PostgreSQL test inspected |
| Entity/repository/query/index/migration | Flyway/JPA and executed query inspected |
| Test class/case and pipeline URL | green pipeline exists |
| Demo fixture and screenshot/reference | integrated `main` path replayed |
| RDS section/figure/table | rendered RDS checked |
| MR/commit/tag | immutable merged reference exists |
| SDS class/sequence/query section | feature-freeze tag inspected |
| Approved deviation | decision issue and propagated document change exist |

Required coverage checks at every gate:

1. Exactly 37 baseline UC rows and no OPT row disguised as committed.
2. Every UC has at least one screen or explicit supporting/non-UI surface and one API/client
   boundary; UC-11 intentionally has no mutation API and UC-03 has no business table.
3. Every mutation cites its authorization, state guard, transaction, idempotency/version
   policy, affected rows, failure guarantee, and at least one adverse test.
4. Every table/query/index traces back to a UC/BR and forward to a test/SDS evidence cell.
5. Every RDS screen/API/database-access table uses the same actor and CRUD semantics here.
6. Every SDS symbol exists in the feature-freeze tag; planned names are never copied as fact.
7. Any blank implementation cell at R5 is either completed or tied to an approved removed UC;
   prose such as “works” is not a substitute for a link or symbol.

## 8. Change protocol

Never edit only this matrix to make a mismatch disappear. A legitimate change records:

```text
decision issue and reason
-> changed scope/UC/BR/state fact
-> screen/API/table/query/NFR impact
-> estimate, owner, dependency and cut offset
-> acceptance/demo impact
-> RDS version and Record of Changes
-> implementation/tests/MR
-> updated matrix
-> SDS deviation or final evidence
```

The reviewer rejects a change that leaves a dangling ID, invents an unowned table, weakens a
stock/money/security invariant to meet a deadline, or silently admits OPT-01/OPT-02.
