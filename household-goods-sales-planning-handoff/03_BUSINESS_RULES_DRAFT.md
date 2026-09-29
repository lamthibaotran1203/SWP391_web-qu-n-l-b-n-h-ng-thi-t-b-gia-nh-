# Business Rules

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **recommended baseline; sequential BR-01 through BR-64**

## Identity and authorization

**BR-01 — Roles.** Persisted human roles are exactly `CUSTOMER`, `STAFF`, and
`MANAGER`. Guest is unauthenticated. `Authenticated User` is a use-case generalization,
not a stored role. Manager inherits Staff operational permissions.

**BR-02 — Acting identity.** Every protected request derives the actor from the Spring
Security session. The client never supplies a trusted actor, role, owner, creator,
approver, receiver, or refunder ID.

**BR-03 — Account state.** Account status is `PENDING_VERIFICATION`, `ACTIVE`, or
`INACTIVE`. Public registration creates a pending Customer. Successful one-use email
verification activates it. Only active accounts may log in. Deactivation invalidates
future authorization and all active sessions. Transition to `ACTIVE` requires a password
hash and verified/setup completion timestamp; an unconfigured pending Staff cannot be
activated by flipping status alone. Normalized email is the login identity and becomes
immutable after activation; baseline has no change-email/re-verification workflow.

**BR-04 — Passwords and tokens.** Passwords are BCrypt hashes and are never returned or
logged. Verification, Staff setup, and reset tokens are cryptographically random, hashed
at rest, single-use, and consumed atomically. Verification and Staff setup tokens expire
after 24 hours; password-reset tokens expire after 30 minutes. Issuing a replacement
invalidates any unused token of the same type. A password change/reset invalidates all
active sessions for that account. Responses to forgotten-password requests do not reveal
whether an email exists. Accepted passwords contain 8–64 characters including at least one
letter and digit, and their UTF-8 encoding may not exceed BCrypt's 72-byte input boundary.

**BR-05 — Staff administration.** UC-08 targets Staff accounts only. Manager may create,
edit safe identity fields, activate/deactivate, and issue a setup token. New Staff starts
`PENDING_VERIFICATION` with no usable password; consuming the 24-hour setup token sets the
first password and activates the account. Reactivating an already configured Staff preserves
their password; UC-07 handles forgotten credentials. Manager may correct email only while
the Staff account is still pending and unconfigured. Manager cannot read/set an existing
password, change an active login email, or use Staff routes to modify Manager accounts. The
first Manager is provisioned by an idempotent deployment bootstrap using environment-supplied
identity/credential values;
there is no public Manager registration or committed default password. Bootstrap never resets
an existing Manager and the initial password variable is removed/rotated after first setup.

**BR-06 — Ownership and privacy.** Customers access only their own profile, addresses,
orders, payments shown through owned orders, and return requests. Staff sees only contact
and address snapshots needed for order operations. No token/password data appears outside
the identity service. Customer profile updates are limited to full name and phone; login
email and role/status are not profile fields.

## Catalogue, SKU, and pricing

**BR-07 — Product aggregate.** Product owns name, description, Category, Brand, one primary
image URL, manufacturer/warranty information text, and active state. SKU represents one
manually defined sale unit and owns immutable SKU code, sale-unit label, whole-VND list price,
reorder threshold, active state, and stock balances. Most Products may have one SKU; a Product
has more than one only when the store actually sells fixed units such as 24 cm and 28 cm.
There is no generic attribute/value model, size-by-color matrix, option generator, or automatic
SKU-combination logic.
Category and Brand names are normalized and case-insensitively unique. An active Category,
active Brand, and at least one active SKU are required before a Product may become active.
Category/Brand deactivation is rejected while an active Product references it. Deactivating
the final active SKU of an active Product is rejected until the Product is deactivated. New
SKUs start with zero on-hand/reserved stock; stock enters through UC-22 or an explicit
opening movement in controlled seed data, never through Product maintenance.

**BR-08 — Active visibility.** Public APIs return only active Products. Active Products may
show active out-of-stock SKUs, but only active SKUs with positive available quantity may be
added as purchasable selections. Product deactivation hides all of its SKUs publicly without
rewriting their rows. SKU deactivation stops new sale selection but does not block fulfillment
of existing orders. Inventory enquiry can include inactive SKUs so remaining stock/history
does not disappear. Deactivation never changes historical snapshots.

**BR-09 — Integer quantities.** Cart, sale, return, receipt, movement, stock, and report
quantities are whole integers. Requested purchase/receipt/return quantities are positive;
stock adjustment delta is a non-zero signed integer. Cart/checkout/counter commands contain
at most 50 distinct SKUs and 1–999 units per line. A stock receipt contains at most 100
distinct SKUs and 1–999,999 units per line. Returns remain capped by sold quantity.

**BR-10 — Authoritative price.** Client catalogue/cart values are informational. Checkout
and counter sale reload locked SKUs and calculate current server price. Tampered, inactive,
missing, or no-longer-available selections are rejected with no partial write.

**BR-11 — VND calculation.** Money uses Java `BigDecimal` and PostgreSQL
`numeric(14,0)`. Baseline sale unit price equals list price. Line amount equals snapshotted
sale unit price times integer quantity. Merchandise amount is the exact sum of lines; total
equals merchandise plus delivery fee. List price is positive; fees and calculated totals are
nonnegative and must fit the shared numeric range. No floating-point money is used.

## Cart and checkout

**BR-12 — Browser-local cart.** Cart is stored in the current browser, not PostgreSQL or
the Spring session. It never reserves stock and may become stale. Login does not merge a
second server cart. Logout does not silently clear the cart; the UI offers an explicit clear.

**BR-13 — Delivery identity.** Delivery checkout requires an active Customer and one active
owned address. Checkout supplies its expected address version; the server locks/reloads that
owned active row before snapshotting recipient, phone, address text, delivery fee, and payment
method. A stale/deactivated address is a 409 with no order or reservation. Later profile/
address changes do not rewrite the order. A Customer has at most one active default address;
setting a new default atomically clears the old one. Default is a UI convenience—checkout
still sends the explicitly selected address.

**BR-14 — Checkout locking.** UC-12 normalizes duplicate SKU lines, sorts selected internal
SKU IDs ascending, locks the selected address first and those SKU rows next in ascending order
within one transaction, then revalidates active state, price, on-hand, reserved, and available
quantity before writing any order row.

**BR-15 — Creation idempotency.** Customer checkout sends a client-generated request key.
The server stores it and a canonical request fingerprint under a uniqueness rule scoped to
the Customer and command. Retrying the same accepted key and fingerprint returns the existing
order; reusing that key with different content is a conflict and reveals no other user's data.

**BR-16 — Order identity and snapshots.** The server generates a globally unique business
order code. Each order item snapshots SKU code/sale-unit label, product name, list price, sale price,
Category name, quantity, and line amount. This classification snapshot makes historical sales
reporting independent of later catalogue edits. Numeric primary keys are never customer-facing
identifiers. After creation, item lines, delivery snapshots, fee, and payment method are never
edited; an ineligible order is cancelled and a new order is placed.

**BR-17 — Fee and payment choice.** Order channel and payment method are different fields.
Channels are `DELIVERY` and `COUNTER`. A DELIVERY order supports exactly payment method `COD`
or `ONLINE`; a COUNTER order uses `CASH`. One fixed server-owned delivery fee is snapshotted.
The baseline fee is 30,000 VND. COD creation starts `CONFIRMED`; ONLINE payment creation
starts `PENDING_PAYMENT`. Counter sale has no delivery fee.

## Inventory

**BR-18 — Stock equation.** Each SKU maintains:

```text
available_quantity = on_hand_quantity - reserved_quantity
on_hand_quantity >= 0
reserved_quantity >= 0
on_hand_quantity >= reserved_quantity
```

`available_quantity` is derived, not independently editable.
`on_hand_quantity` means sellable units controlled by this one stock location. Damaged/
non-sellable returned units remain disposition evidence in `return_item`; they are not a
second inventory pool in the baseline.

**BR-19 — Movement ledger.** Every physical or reservation mutation appends immutable
inventory movement rows containing SKU, on-hand delta, reserved delta, after-balances,
movement type, business reference, actor/system source, and timestamp. Movement rows are
never edited or deleted through the application. Any non-zero initial/demo stock is introduced
with an `OPENING_BALANCE` movement; seed scripts may not set an unexplained balance directly.

**BR-20 — Stock receipt.** One receipt has a unique request key, source name, optional
external reference, and at least one unique active SKU line with positive quantity. Header,
lines, balances, and movements commit atomically. The operator-scoped key and canonical
request fingerprint distinguish an exact retry from conflicting content. It does not create
Supplier or Purchase Order entities.

**BR-21 — Stock adjustment.** Only Manager adjusts stock. Request includes expected SKU
version, a non-zero delta, and reason. Final on-hand cannot be negative or lower than current
reserved quantity. A stale version or invalid final balance rejects the entire command.

**BR-22 — Reservation effects.** Placing a delivery order increases reserved quantity.
Eligible cancellation and pending-payment expiration decrease it. Packing changes no stock.
All affected SKU rows are locked in ascending ID order.

**BR-23 — Physical sale effects.** Dispatch decreases both on-hand and reserved quantities.
Counter sale decreases on-hand only. Successful delivery changes no stock because dispatch
already recorded departure.

**BR-24 — Goods returning to store.** Failed delivery is recorded only after the whole
parcel physically returns and increases on-hand only; the order becomes terminal and no stock
is re-reserved. Customer-return intake increases on-hand only for sellable quantities.
Non-sellable quantities are recorded in return items but never enter sellable stock. The
baseline treats a failed-delivery parcel as returned sellable; exceptional loss/damage is
recorded afterward through Manager stock adjustment with a reason, not a second delivery state.

## Order lifecycle

**BR-25 — Order status and channel.** `DELIVERY` order statuses are `PENDING_PAYMENT`,
`CONFIRMED`, `PACKING`, `SHIPPING`, `COMPLETED`, `DELIVERY_FAILED`, and `CANCELLED`.
A sale with channel `COUNTER` is created directly in status `COMPLETED`.

**BR-26 — Valid transitions.** Only these delivery transitions are permitted:

```text
PENDING_PAYMENT -> CONFIRMED or CANCELLED
CONFIRMED       -> PACKING or CANCELLED
PACKING         -> SHIPPING or CANCELLED by Manager
SHIPPING        -> COMPLETED or DELIVERY_FAILED
```

`DELIVERY_FAILED`, `COMPLETED`, and `CANCELLED` are terminal. The baseline has no
redispatch or backward transition.

**BR-27 — Status history.** Creation and every successful transition append an immutable
from/to status, actor/system source, timestamp, and required reason/reference. Rejected or
failed commands append nothing.

**BR-28 — Customer cancellation.** Customer may cancel an owned `PENDING_PAYMENT` or
`CONFIRMED` order. The command releases reservation and marks any open payment attempt
`EXPIRED`. Any collected amount becomes refund due; cancellation never claims money was
already returned. A later verified provider success is still handled by BR-35.

**BR-29 — Store cancellation.** Manager may cancel `PENDING_PAYMENT`, `CONFIRMED`, or
`PACKING` with reason. `SHIPPING` cannot be cancelled; Staff must record success or a
physical returned failure. Store cancellation releases reservation, marks any open payment
attempt `EXPIRED`, and exposes paid value as refund due. A later verified provider success
is still handled by BR-35.

**BR-30 — Terminal order and returns.** Terminal orders are not edited or reopened.
`COMPLETED` may own one separate Return Request; return state never changes the order out of
`COMPLETED`.

**BR-31 — Order views and actions.** Customer queries enforce ownership. Staff/Manager
queries may read operational snapshots and histories. Allowed actions are derived from
current role, channel, status, payment/refund sums, and return state; they are not stored as
booleans sent by the client.

## Online payment, collection, and refund

**BR-32 — Payment attempts.** Each online attempt stores unique attempt code, provider,
provider references when available, expected amount/currency, status, created/updated/expiry
times, and order. Attempt statuses are `CREATED`, `PENDING`, `SUCCEEDED`, `FAILED`, or
`EXPIRED`. A verified result may move `EXPIRED -> SUCCEEDED` without reopening its cancelled
order. Retry creates a new attempt; an order has at most one non-final open attempt.

**BR-33 — External boundary.** Redirect creation and provider HTTP calls occur without
holding order/SKU database locks. A short transaction prepares the attempt; provider work
occurs outside it; a second short locked transaction verifies and finalizes state.

**BR-34 — Verified idempotent success.** Return/IPN handling verifies signature, merchant,
currency, order reference, exact amount, and provider success code. A unique provider
transaction reference plus locked status guard creates at most one collection and one
`PENDING_PAYMENT -> CONFIRMED` transition. A browser redirect alone is never payment proof;
only the backend's verified provider payload may finalize the attempt.

**BR-35 — Failure, expiry, and late success.** Failed attempts may be retried before order
expiry. The baseline payment/reservation window ends exactly 15 minutes after order creation.
The first locked command or verified callback handled at/after that server-Clock instant must
apply expiry before considering confirmation, so scheduler timing cannot change the result.
Expiry cancels an unpaid `PENDING_PAYMENT` order, releases reservation, records reason
`PAYMENT_EXPIRED`, and marks open attempts expired. A later valid success moves its matching
attempt to `SUCCEEDED`, records collection once, never reopens the cancelled order, and makes
the full collected amount refund due.

**BR-36 — Money ledger.** Every collection/refund is an immutable positive-amount
`payment_transaction` with a server-generated unique transaction code, order, type, method,
source/actor, external/manual reference where applicable, and recorded time. Derived collected,
refunded, and net values come only from this ledger.

**BR-37 — Receipt eligibility.** Paid receipt is available only when successful collections
cover the order total under its payment rule. Unpaid COD order has an order summary, not a
paid receipt. Receipt content is generated from immutable sale/payment snapshots; no invoice
table exists. Store name/address/phone are deployment configuration, not an editable Store
module; the output is a sales/payment receipt, not a statutory e-invoice. A CANCELLED or
DELIVERY_FAILED paid order visibly shows its terminal and refund state so the output cannot be
misread as a completed sale.

**BR-38 — Refund cap.** For paid `CANCELLED` or `DELIVERY_FAILED` orders, refundable cap is
the collected amount. For a `RECEIVED` return, cap is received quantity times snapshotted
sale unit price. Delivery fee is excluded from return refund. Cumulative refunds cannot
exceed the applicable cap or cumulative collections for the order.

## Fulfillment and counter sale

**BR-39 — Packing.** Only `CONFIRMED` becomes `PACKING`. The command locks the order,
records operator/time history, and changes no stock.

**BR-40 — Dispatch.** Only `PACKING` becomes `SHIPPING`. Delivery reference is required.
The transaction locks order then order SKUs ascending and atomically updates status/history,
on-hand/reserved balances, and dispatch movements.

**BR-41 — Delivery outcome.** Only `SHIPPING` accepts an outcome. Success records delivery
time and becomes `COMPLETED`. Failure requires reason and confirmation that the parcel has
returned, restores on-hand quantity, and becomes terminal `DELIVERY_FAILED`.

**BR-42 — COD completion.** COD success requires exact collection in the same transaction
as `SHIPPING -> COMPLETED`. If collection cannot be recorded, the order does not complete.
ONLINE completion does not create another collection.

**BR-43 — Counter sale.** UC-29 uses a unique request key, locks selected active SKUs in
ascending order, reloads price/available stock, and atomically creates a completed COUNTER
order/items/history, cash collection, stock balances/movements, and receipt. Customer identity
is optional; order/receipt code is sufficient for return evidence. The operator-scoped key
and canonical request fingerprint distinguish exact retry from conflicting content. A committed
counter sale is not edited or voided; corrections use the normal eligible return/refund path.
Its `created_at`, `completed_at`, collection, history, and movement use one service Clock
instant so report boundaries cannot split one atomic sale.

## Returns

**BR-44 — Eligibility.** Return request targets a `COMPLETED` order at an instant strictly
before `completed_at + 7 days`; the API exposes the resulting deadline in shop time. Allowed
reasons are `DEFECTIVE`, `WRONG_ITEM`, and `SHIPPING_DAMAGE`. COUNTER and DELIVERY sales are
both eligible through their proper request channel. The deadline is checked when the request
is created; a timely request may be reviewed, received, and refunded after that deadline.

**BR-45 — One request and item quantities.** An order has at most one Return Request. It
contains at least one distinct order-item line and positive requested quantity not exceeding
sold quantity. An actor-scoped request key and canonical request fingerprint prevent duplicate
creation and detect conflicting reuse; the unique order relationship is the final race guard.

**BR-46 — Request channels.** Return-request channels are `CUSTOMER` and `IN_STORE`.
Customer may request only from an owned DELIVERY order. Staff/Manager may create an IN_STORE
request after validating a receipt/order code and presented items, including a COUNTER sale.
These remain separate UCs because actor, evidence, authorization, and screen differ.

**BR-47 — Return lifecycle.** Valid transitions are:

```text
REQUESTED -> APPROVED -> RECEIVED -> REFUNDED
          -> REJECTED
```

`REJECTED` and `REFUNDED` are terminal. There is no withdrawal, appeal, multi-intake, or
second request.

**BR-48 — Decision.** Only Manager reviews `REQUESTED`. Manager approves the full set of
requested lines/quantities or rejects the whole request. Rejection requires reason; approval
may include an inspection note. Partial approval is outside the baseline.

**BR-49 — Receipt and disposition.** Only `APPROVED` may be received. Staff performs one
intake and records, per line, received, sellable, and non-sellable quantities where sellable
plus non-sellable equals received and received does not exceed requested. Total received over
the request must be positive; any approved quantity not presented is closed with this one
intake and cannot arrive later. Sellable stock and movement rows update atomically with
`RECEIVED`.

**BR-50 — Refundable merchandise.** Return refund uses received quantity times the order
item's snapshotted sale unit price. Current product price never reprices a return.

**BR-51 — Delivery fee.** Delivery fee is never refundable through the baseline return
workflow. A full order cancellation/delivery failure remains governed by BR-38.

**BR-52 — Refund completion.** Only Manager records money after it is actually returned by
cash or manual bank transfer. Amount must equal the remaining eligible cap for the selected
cancellation/failure/return source. Refund transaction and `RECEIVED -> REFUNDED` transition,
when applicable, commit atomically. Method and evidence reference are required; a cash refund
uses a server-generated refund receipt code. Provider refund API is excluded.

**BR-53 — Return exclusions.** No exchange, replacement shipment, warranty repair, store
credit, goodwill refund without received goods, partial negotiation, second intake, second
request, or Manager window override exists.

## Reports

**BR-54 — Cash movement report.** Inclusive local dates become a half-open instant range
over `payment_transaction.recorded_at`. Report groups positive collections and refunds by
local date and method and computes `net_cash_movement = collected - refunded`. It is not
accounting profit or accrued sales and does not filter by current order status.

**BR-55 — Sales performance report.** Cohort contains orders whose `completed_at` lies in
the selected instant range, for both DELIVERY and COUNTER channels and their payment methods.
It reports gross completed count, merchandise value, delivery fee, total sale value, average
order, and product/SKU/category quantities from sale snapshots. Later returns/refunds do not
rewrite this historical gross-sale cohort; it is not relabeled as cash or net revenue.

**BR-56 — Inventory roll-forward report.** For each SKU and movement-time range, opening
sellable-on-hand/reserved balances plus categorized deltas reconcile exactly to closing
balances. Historical stock is derived from movement rows, never current order status.

**BR-57 — Query boundaries.** Inventory Overview and the actionable fulfillment preset in
Sales Order Search are current-state operational views. Stock Movement History returns individual events. The three reports use
transaction/completion/movement time respectively. Shared tables do not justify duplicate
screens with the same result.

## Time, persistence, API, and traceability

**BR-58 — Shop time.** Event times use Java `Instant` and PostgreSQL `timestamptz`. UI dates
use `Asia/Ho_Chi_Minh`. Inclusive date filters convert to `[startOfDay, startOfNextDay)`.
One atomic business command captures one Clock instant for all of its state/history/ledger
rows. Tests use an injected fixed `Clock`; server OS timezone is irrelevant.

**BR-59 — Serialized commands.** Existing order/payment/return commands lock their owning
aggregate before reloading status, sums, and guards. SKU-changing commands lock SKUs in
ascending internal ID order. Transactions contain no email or provider network wait.

**BR-60 — Schema ownership.** Flyway owns all DDL in PostgreSQL schema `app`; Hibernate uses
`ddl-auto=validate`. Database checks protect row-level invariants and services protect
cross-row/lifecycle invariants. Every foreign key and frequent filter/sort path is reviewed
for an index.

**BR-61 — Retention.** Referenced accounts, catalogue records, orders, items, payment rows,
movements, receipts, and returns are not hard-deleted. Active/status fields or correcting
ledger rows preserve history. Tokens may be purged after their retention window.

**BR-62 — Session and API security.** Production React and `/api/v1` are same-origin.
Session cookies are HttpOnly, Secure in HTTPS, and SameSite=Lax. Every unsafe authenticated
request carries a valid CSRF token. Frontend role guards are navigation help only; backend
route and ownership checks are authoritative. Baseline idle session timeout is 30 minutes;
there is no remember-me session.

**BR-63 — Client trust and errors.** APIs accept DTOs, not JPA entities. Client totals,
prices, role/owner IDs, statuses, balances, refund caps, and allowed-action flags are
untrusted. Validation errors return 400, unauthenticated 401, forbidden 403, missing 404,
state/version/idempotency conflicts 409, and unexpected failures a safe correlation ID.

**BR-64 — Traceability.** A change is incomplete until Scope, UC catalogue, BR/state model,
screen/API/data/query/test contracts, GitLab backlog, RDS, demo data, and later SDS agree.
SDS documents implemented, inspected, and tested behavior only.
