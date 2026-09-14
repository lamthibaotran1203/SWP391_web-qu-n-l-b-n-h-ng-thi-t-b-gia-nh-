# Scope Audit and Use Case Rationale

Status: **third-pass audit of the React/Spring 37-UC baseline**

## Verdict

The recommended commitment is balanced for five students and ten weeks **only when the
optional features remain outside the promise until the COD core passes**. The baseline has
37 actor-goal UCs, 86 relative effort points, seven epics, approximately 25 routed screens,
and 17 business tables.

This is not a small CRUD catalogue. Its depth comes from four coherent vertical paths:
customer sale, online payment, store fulfillment/counter sale, and return/refund, all tied to
an inventory and money ledger. It is also not production-scale because it fixes one store,
one location, one provider, one delivery rule, one return intake, and explicit exclusions.

The earlier 41-UC verdict depended on Thymeleaf. React adds a separate view model, API
boundary, client state, CSRF lifecycle, frontend tests, and build packaging. Keeping the old
count without simplifying behavior would spend the integration buffer before defects and
documents.

## Corrections made in this audit

| Previous shape | Audit decision | Why it is safer without becoming shallow |
| --- | --- | --- |
| UC-22 Promotion in baseline | Move to OPT-01 | Pricing remains correct at list price; extension boundary remains explicit |
| UC-37 Dashboard in baseline | Move to OPT-02 | Fulfillment, return, and low-stock views already expose current work |
| Paid receipt as standalone UC | Merge into customer/staff order detail and counter-sale output | Same authorization/reference and no independent business mutation |
| Failed delivery could redispatch | Make `DELIVERY_FAILED` terminal after parcel return | Removes re-reservation and second dispatch cycle while preserving truthful stock/state |
| Multiple returns over time | One item-level request per order | Keeps multi-item realism and removes cumulative cross-request races |
| Partial approval | Manager approves all requested lines or rejects | Keeps separation of decision/intake/refund without quantity negotiation matrix |
| Conditional delivery-fee refund | No delivery-fee refund for returns | Removes a fragile exception; full cancellation/failure still refunds collected order value |
| Session cart | Browser-local cart | React owns temporary UI state; checkout remains fully authoritative and needs no cart table |
| Thymeleaf/PRG routes | Same-origin React + JSON APIs | Satisfies selected stack while retaining session/CSRF and one deployment |
| Candidate Customer Profile table | Contact fields on `user_account` | Staff and Customer share safe identity fields; one fewer join/table |
| Separate Invoice/Refund tables were possible | Receipt is projection; refund is money-ledger row | Avoids duplicate sources of truth |
| Standalone fulfillment queue | Merge into UC-17 as an actionable status preset | Same actor, dataset, filters, route, and observable enquiry result |
| Generic product variants | Keep manual sale-unit SKUs only | Supports fixed household-goods units without attribute/value tables or combination logic |

## Defensible UC boundaries

### Keep customer list and detail separate

Order history has paging/filtering and a compact projection. Order detail has ownership,
timeline, payment/receipt, allowed actions, and return eligibility. They differ in trigger,
data, and tests.

### Keep staff list and detail separate

Staff search crosses channels/statuses and returns compact operational rows. Detail exposes
address/payment/stock/history/action data. Combining them produces one large endpoint and
screen contract that is harder to secure and test.

The actionable fulfillment queue is not a third goal. It is a saved filter and default sort
inside staff search for `CONFIRMED`, `PACKING`, and `SHIPPING` orders.

### Keep customer and store cancellation separate

Actors and allowed statuses differ. Manager cancellation requires reason and includes
PACKING; Customer cancellation requires ownership and excludes PACKING. Sharing a service
policy is valid, sharing one UC is not.

### Keep inventory overview, movement history, and report separate

- Overview answers current availability and low stock.
- Movement history investigates individual events.
- Report reconciles opening plus period deltas to closing.

They share tables but not trigger, time basis, granularity, or result.

### Keep fulfillment commands split

Pack, dispatch, and delivery outcome occur at different physical times. Dispatch changes
stock; pack does not. Delivery success may collect COD; returned failure restores stock.
Merging them would hide transaction and state boundaries the team must defend.

### Keep two return-request channels

Customer proves order ownership online. Staff validates a presented receipt/order in store.
They create the same aggregate but have different actor, evidence, UI, authorization, and
failure paths.

### Keep decision, receipt, and refund separate

Approval grants permission, receipt moves goods, and refund moves money. They happen at
different times and have different authorized actors/effects. Their separation is the key
explainable control in the return module.

### Keep three reports separate

- Cash report uses money transaction time.
- Sales report uses order completion time.
- Inventory report uses movement time.

One combined “Revenue Dashboard” would produce ambiguous totals and weak RDS test oracles.

## Correctly merged behavior

- Category and Brand actions share one small Manager classification workspace.
- Product and SKU management share one aggregate; orphan SKU administration is prohibited.
- Add/update/remove/clear cart actions share one visitor goal.
- Receipt rendering is a conditional view/output of order detail and counter sale.
- Gateway return/IPN, duplicate handling, and expiration support UC-13.
- Current allowed actions are calculated in order detail, not separate permission UCs.
- The fulfillment queue is an actionable preset inside staff order search.
- Status history, inventory movement, and request-key handling are transaction effects.

## Deliberate non-features

### Procurement and supplier master

Stock receipt keeps source name/reference. Adding Supplier without Purchase Order/payment is
shallow; adding the full procurement package is a major second system. Both are excluded.

### Warranty and exchange

Seven-day defect/wrong/damage return is enough for retail after-sales depth. Warranty repair
requires claim, manufacturer/service centre, repair states, and handover. Exchange requires
a second sale allocation. Neither belongs in this term.

### Notification and realtime

Account email is infrastructure for identity only. Order notifications require templates,
delivery attempts, read state, and retry policy. Customers can inspect current order state.
No WebSocket/Realtime feature is promised.

### Direct Supabase client access

All browser access goes through Spring Boot. Adding Supabase Auth/Data API/RLS beside Spring
Security creates two authorization models and makes pooled user context harder to explain.
The application uses a private `app` schema and least-privilege JDBC role instead.

### JWT and split deployment

React does not require JWT. Same-origin session authentication avoids access/refresh token
storage, rotation, revocation, and cross-site cookie configuration. A second frontend host is
future deployment work, not a course requirement.

## Complexity budget

| Area | Included depth | Explicit ceiling |
| --- | --- | --- |
| Identity | verification/reset/setup tokens, sessions, CSRF, role/ownership | no social login, MFA, blacklist, device/session manager |
| Catalogue | Product/SKU, Category/Brand, active state, image URL | no upload service, bulk import, reviews, promotion baseline |
| Checkout | snapshots, idempotency, stock lock/reservation, fixed fee | no tax, coupons, zones, guest checkout |
| Payment | one sandbox, signature/amount/idempotency, expiry/late success | no multiple providers, automatic refunds, reconciliation UI |
| Inventory | balance plus immutable movements, receipt, adjustment | no supplier/PO, warehouse/transfer, batch/serial tracking |
| Fulfillment | pack, dispatch, success/returned failure, COD | no courier assignment/tracking, redispatch, partial shipment |
| Counter sale | anonymous cash sale with receipt | no split payment, cash drawer/shift, barcode hardware |
| Return/refund | one multi-item request, decision, one intake, disposition, manual refund | no exchange, second request/intake, partial approval, delivery-fee refund |
| Reports | three exact cohorts and table/chart-lite output | no BI builder, forecasting, export suite, accounting P&L |

## Highest-risk slices and mandatory proof

| Slice | Failure to prevent | Minimum evidence |
| --- | --- | --- |
| React session/CSRF | login works but POST fails after auth/logout | login -> mutation -> logout -> fresh token integration test |
| UC-12 checkout | oversell, stale price, partial write, duplicate order | concurrent checkout, rollback, request-key retry tests |
| UC-13 payment | forged/duplicate/late result | signature, exact amount/reference, duplicate, expiry race tests |
| UC-16/28 cancellation | reservation or money state wrong | every allowed/forbidden status plus paid-late case |
| UC-22/23 stock commands | balance and ledger drift | transaction rollback and independent movement reconciliation |
| UC-25/27 fulfillment | state and physical stock disagree | dispatch/success/failure/COD atomicity tests |
| UC-29 counter sale | cash row exists without stock/order | concurrent stock and full rollback tests |
| UC-30–34 return/refund | quantity, disposition, stock, and cap disagree | one-request, line caps, disposition, exact-refund tests |
| UC-35–37 reports | wrong timestamp or double-count | fixed-clock fixtures and independent SQL/result tie-out |

## Feasibility gates

- End week 2: if React/session/CSRF/Flyway/PostgreSQL walking skeleton is not green, freeze
  all feature branches and repair the skeleton.
- End week 4: if COD checkout cannot survive concurrent stock tests, optional features stay
  permanently out and strongest two members pair on the transaction.
- End week 6: if fulfillment/counter/stock ledgers do not reconcile, postpone UC-07 before
  weakening stock correctness.
- End week 8: if reports are late, reduce charts/export polish but keep exact table outputs
  and all three time meanings.
- End week 9: feature freeze is absolute; only defects, tests, documents, demo data, and
  rehearsal remain.

## Scope admission test

A proposal may enter baseline only when it identifies actor, trigger, observable result,
non-overlapping UC boundary, state/data effects, API/screen/test changes, owner/reviewer,
estimate, and a removed item of comparable capacity. “Real stores have it” is not evidence.
