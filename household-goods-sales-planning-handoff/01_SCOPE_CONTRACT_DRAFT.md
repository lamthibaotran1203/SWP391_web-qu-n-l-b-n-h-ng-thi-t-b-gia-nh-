# Household Goods Sales and Inventory Management System Scope Contract

Status: **recommended baseline for team sign-off**

Delivery constraint: five students, ten weeks

Committed catalogue: 37 actor-goal use cases; Optional: 2

## Product statement

The system supports sales and inventory operations for one small Vietnamese household-
goods store and one stock location. Guests browse products and keep a browser-local cart.
Registered Customers place delivery orders using COD or one VND-capable sandbox payment
gateway. Staff fulfill orders, receive stock, sell at the counter, search orders, and receive
approved returns. A Manager controls staff accounts, catalogue data, stock correction,
store cancellation, return decisions, actual refund recording, and reports.

The product is not a marketplace, multi-branch ERP, procurement suite, carrier platform,
warranty centre, accounting system, or social-commerce application.

## Required demonstration paths

### Online COD sale

```text
Guest browses -> registers/logs in -> keeps cart -> selects address
-> places COD order -> stock is reserved
-> Staff packs -> dispatches -> records successful delivery and exact COD collection
-> order becomes COMPLETED
-> Customer sees detail/receipt and Manager reconciles sales and cash reports
```

### Online payment sale

```text
Customer places a DELIVERY-channel order with ONLINE payment
-> stock is reserved under PENDING_PAYMENT for 15 minutes
-> gateway result is signature/reference/amount checked and stored once
-> order becomes CONFIRMED -> Staff fulfills -> order becomes COMPLETED
-> Customer sees payment state and receipt
```

### Counter sale

```text
Staff selects SKUs/quantities -> server reloads price and available stock
-> Staff confirms cash received
-> one transaction creates a completed COUNTER order, collection, stock movements,
   status history, and printable receipt
```

### Item-level return and refund

```text
Customer requests selected items from an owned completed online order
or Staff starts an in-store request from a receipt
-> Manager approves the complete request or rejects with reason
-> Staff receives once and records sellable/non-sellable quantities
-> sellable stock is restored
-> Manager records the exact eligible refund actually returned
-> return becomes REFUNDED; the original order remains COMPLETED
```

## Frozen baseline decisions

- One store, one stock location, and `Asia/Ho_Chi_Minh` business timezone.
- UI/errors/demo data are Vietnamese. Code, schema, routes, UC/BR/NFR IDs, RDS, and SDS
  identifiers are English.
- Products use integer SKU quantities; no weighted stock.
- Product contains public content. Each SKU is one manually defined sale unit with a label,
  price, stock, and reorder threshold. The baseline has no size/color/attribute matrix.
- Cart lives in browser storage and never reserves stock. Every checkout value is untrusted.
- A verified Customer account is required for delivery checkout.
- Exactly COD and one VND-capable sandbox online provider are supported.
- Order channels are `DELIVERY` and `COUNTER`; payment methods are separate values. A
  DELIVERY order uses COD or ONLINE, while a COUNTER order uses CASH.
- Counter sale is cash only and may be anonymous; receipt/order code is its return evidence.
- No baseline promotion. List price equals sale price while snapshot fields still preserve
  a future extension boundary.
- One fixed local delivery fee of 30,000 VND is calculated by the server and snapshotted.
- Staff records delivery outcomes. Failure is recorded only after the parcel returns, sets
  terminal `DELIVERY_FAILED`, restores physical stock, and has no redispatch loop.
- Returns apply before the exact instant `completed_at + 7 days` for defective, wrong, or
  shipping-damaged goods.
- One order may have at most one return request. It can contain multiple item quantities.
- Manager approves the whole request or rejects it; Staff performs one receipt/inspection.
- Refund uses received quantities and sold-price snapshots. Delivery fee is not refundable.
- Refund is recorded only after cash/manual bank transfer is actually completed. No provider
  refund API exists.
- Completed orders remain completed when a Return aggregate exists.
- Active/status fields and reversing/ledger rows replace destructive deletion for referenced
  business data.
- React and Spring Boot are one same-origin production application. Browser code never
  receives database credentials or connects directly to Supabase.

## Included baseline capabilities

- Registration/verification, login/logout, profile/address/password maintenance, password
  reset, and Staff account administration.
- Public catalogue/search/detail and browser-local cart.
- COD/online-payment checkout, customer order history/detail/receipt/cancellation.
- Staff order search/detail with an actionable fulfillment filter, packing, dispatch,
  delivery outcome, and Manager store cancellation.
- Category/Brand/Product/SKU administration.
- Current inventory, stock receipts, Manager adjustments, immutable movement history.
- Cash counter sales.
- Customer online return request, Staff in-store request, Manager decision, one physical
  intake/inspection, and actual refund completion.
- Cash-movement, completed-sales, and inventory roll-forward reports.

## Optional features after the core gate

| Optional ID | Feature | Admission gate | Required cut if it threatens schedule |
| --- | --- | --- | --- |
| OPT-01 | Automatic SKU promotion campaigns | R2 COD flow, stock reconciliation, and core CI are green | Remove feature and promotion tables/fields; list price remains authoritative |
| OPT-02 | Current operational dashboard | R3 fulfillment/return queues already produce correct counts | Remove page; queues and low-stock filter remain |

Optional items are not included in the 37 baseline count, workload promise, RDS main UC
catalogue, or defense demo until formally admitted.

## Explicit exclusions

- Marketplace sellers, multiple stores/warehouses, stock transfer, or consolidated reports.
- Supplier master, purchase order, procurement approval, supplier payment, or accounts
  payable. Stock receipt stores only source name/reference.
- Large installed appliances, delivery scheduling slots, installation, maintenance, repair,
  or warranty-claim workflow.
- Exchange/replacement, store credit, loyalty, points, wishlist, reviews, ratings, chat,
  tickets, or customer-service workflow.
- Coupon codes, stacked/personalised/dynamic pricing, tax, accounting, e-invoice, or manual
  cashier discount.
- Carrier API, routing/maps, driver tracking, or Courier account.
- SMS/Zalo/order-email notifications. Email is limited to account setup/verification/reset.
- Automatic provider refunds, partial refund negotiation, delivery-fee refund, multi-intake
  returns, second return for the same order, or post-window exception override.
- Supabase Auth/Data API/Realtime/Storage from the React client, JWT, cross-origin production
  frontend, microservices, Redis, Kafka, generic workflow engine, or policy editor.
- AI, recommendation engine, chatbot, computer vision, IoT/barcode hardware, native mobile,
  CSV import, bulk catalogue import, and export-heavy analytics.

An exclusion may appear under Future Work but not as an implemented UC, hidden button,
speculative table/field, fake integration, or unfinished demo promise.

## Ready-for-RDS gate

Detailed RDS authoring begins only when:

1. Team signs the baseline decisions and optional-feature policy.
2. UC-01 through UC-37 names/actors/priority are accepted and sequential.
3. BR-01 through BR-64, order/payment/return/stock transitions, and report cohorts agree.
4. The payment and SMTP sandboxes are selected or time-boxed behind explicit stubs that do
   not claim successful external delivery.
5. Every baseline UC maps to a screen or non-UI entry, API, service transaction/query,
   table set, acceptance test, demo evidence, owner, and reviewer.
6. The 17-table logical model has an owner and no untraced table.
7. The GitLab backlog contains no System-only or technical-padding use cases.
