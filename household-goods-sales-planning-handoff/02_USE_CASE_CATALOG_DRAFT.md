# Use Case Catalogue

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **recommended 37-UC baseline for team sign-off**

## Classification rule

A use case is an external actor's goal with a trigger and observable business result.
Validation, calculations, snapshots, row locks, callbacks, expiration jobs, stock movements,
history insertion, receipt rendering, and report SQL remain inside the actor goal they
support.

`Manage` is used only when the actions share actor, authorization boundary, workspace,
aggregate/reference-data purpose, and result. List/detail remain separate when they differ
in trigger, pagination, data exposure, actions, or reuse. `Authenticated User` is an abstract
actor generalized by Customer, Staff, and Manager, not a persisted role.

Priority meanings:

- `P0`: coherent core and critical path; must be stable before optional work.
- `P1`: committed final baseline after the P0 vertical path is green.
- `Optional`: excluded from the 37-UC commitment until formally admitted.

Effort uses relative points only: Low = 1, Medium = 2, High = 3.

## Baseline catalogue

| ID | Module | Primary actor | Use case | Observable result | Priority | Effort |
| --- | --- | --- | --- | --- | --- | ---: |
| UC-01 | Identity | Guest | Register Customer Account | Visitor verifies email and receives an active Customer account | P0 | 2 |
| UC-02 | Identity | Customer/Staff/Manager | Login | Active user receives a secure role-appropriate session | P0 | 2 |
| UC-03 | Identity | Authenticated User | Logout | Current session ends and protected APIs reject it | P0 | 1 |
| UC-04 | Customer account | Customer | Manage Personal Profile | Customer views and updates own allowed contact fields | P1 | 2 |
| UC-05 | Customer account | Customer | Manage Delivery Addresses | Customer creates, edits, selects default, and deactivates own addresses | P0 | 2 |
| UC-06 | Credential security | Authenticated User | Change Own Password | User replaces password after proving the current password | P1 | 2 |
| UC-07 | Credential recovery | Guest | Reset Forgotten Password | Account owner uses a one-use expiring email token to replace password | P1 | 2 |
| UC-08 | Staff administration | Manager | Manage Staff Accounts | Manager creates/edits/activates/deactivates Staff and issues setup access | P1 | 2 |
| UC-09 | Product discovery | Guest/Customer | Browse and Search Products | Visitor receives a paged active catalogue matching filters/sort/search | P0 | 2 |
| UC-10 | Product discovery | Guest/Customer | View Product Detail | Visitor sees one active product, purchasable SKUs, price, and availability | P0 | 1 |
| UC-11 | Shopping cart | Guest/Customer | Manage Shopping Cart | Visitor adds, changes, removes, or clears browser-local SKU quantities | P0 | 2 |
| UC-12 | Customer ordering | Customer | Place Customer Order | Server revalidates address/price/stock/payment, reserves stock, snapshots sale, and creates one order | P0 | 3 |
| UC-13 | Online payment | Customer | Complete Online Payment | One verified gateway result records collection once and confirms the pending order | P1 | 3 |
| UC-14 | Customer enquiry | Customer | View My Order History | Customer sees only own orders with paging and filters | P0 | 1 |
| UC-15 | Customer enquiry | Customer | View Own Order Detail and Receipt | Customer sees owned order/payment/timeline/return state and paid receipt when eligible | P0 | 2 |
| UC-16 | Customer cancellation | Customer | Cancel Eligible Own Order | Eligible pending/confirmed order is cancelled, reservation released, and paid value becomes refund due | P0 | 3 |
| UC-17 | Staff order enquiry | Staff/Manager | View and Search Sales Orders | Operator searches all channels and lifecycle states by business criteria | P0 | 2 |
| UC-18 | Staff order enquiry | Staff/Manager | View Sales Order Detail | Operator sees operational, stock, payment, return, receipt, audit, and allowed actions | P0 | 2 |
| UC-19 | Product administration | Manager | Manage Product Classification Data | Manager maintains Category and Brand reference data in one workspace | P1 | 2 |
| UC-20 | Product administration | Manager | Manage Product Catalogue and SKUs | Manager maintains product content and manually defined sale units/SKUs with price, threshold, and active state | P0 | 3 |
| UC-21 | Inventory enquiry | Staff/Manager | View Inventory Overview | Operator searches on-hand, reserved, available, and low-stock SKU balances | P0 | 2 |
| UC-22 | Inventory receiving | Staff/Manager | Receive Stock | Operator records one external delivery and atomically increases its SKU balances | P0 | 3 |
| UC-23 | Inventory correction | Manager | Adjust Stock | Manager records a reasoned non-zero correction without violating reservations | P0 | 2 |
| UC-24 | Inventory audit | Manager | View Stock Movement History | Manager inspects immutable physical/reservation movements by SKU/order/reference | P1 | 2 |
| UC-25 | Fulfillment | Staff/Manager | Pack Confirmed Order | Eligible confirmed order becomes packing with actor/time history | P0 | 1 |
| UC-26 | Fulfillment | Staff/Manager | Dispatch Order for Delivery | Eligible packed order leaves stock and becomes shipping | P0 | 3 |
| UC-27 | Fulfillment | Staff/Manager | Record Delivery Outcome | Shipping order becomes completed after success or terminal delivery-failed after returned failure | P0 | 3 |
| UC-28 | Store cancellation | Manager | Cancel Order by Store | Manager cancels an allowed pre-shipping order with reason, stock release, and refund-due result | P0 | 3 |
| UC-29 | Counter sales | Staff/Manager | Sell Products at Counter | One atomic command creates a completed cash sale, collection, stock movements, and receipt | P0 | 3 |
| UC-30 | Customer returns | Customer | Request Return for Own Order | Customer creates the one allowed item/quantity request for an eligible owned DELIVERY order | P1 | 3 |
| UC-31 | In-store returns | Staff/Manager | Create In-Store Return Request | Operator creates the one allowed item/quantity request after validating receipt/order evidence | P1 | 3 |
| UC-32 | Return decision | Manager | Review Return Request | Manager approves the complete eligible request or rejects it with reason | P1 | 2 |
| UC-33 | Return receiving | Staff/Manager | Receive and Inspect Returned Items | Operator performs one intake, records disposition quantities, and restores sellable stock | P1 | 3 |
| UC-34 | Refund management | Manager | Record Refund Completion | Manager records actual eligible money returned for a cancellation/failure/received return without exceeding cap | P1 | 3 |
| UC-35 | Reporting | Manager | View Cash Movement Report | Manager sees collections, refunds, and net cash movement by transaction date/method | P1 | 3 |
| UC-36 | Reporting | Manager | View Sales Performance Report | Manager sees completed sales volume/value/channel/product demand by completion date | P1 | 3 |
| UC-37 | Reporting | Manager | View Inventory Roll-Forward Report | Manager sees opening balances plus period deltas reconciling to closing balances | P1 | 3 |

Total relative load: **86 points**. The number is for balancing only, not a Scrum estimate
or teacher-facing score.

## Supporting behavior that is not a separate UC

- Email verification/resend is part of UC-01; Staff setup email is part of UC-08.
- `GET /auth/me`, CSRF token refresh, role navigation, and route guards support login/session
  flows; they are not actor goals.
- Cart persistence in browser storage, price display, delivery-fee calculation, order-code
  generation, idempotency key handling, sale snapshots, reservation, and initial history are
  steps of UC-11/UC-12.
- Payment redirect, return URL, IPN/callback, signature verification, duplicate result,
  retry, expiry, and late success support UC-13.
- Paid receipt rendering is a conditional output of UC-15/UC-18/UC-29.
- The fulfillment queue is an actionable status preset inside UC-17 and SCR-15, not a
  separate actor goal, route, or screen.
- Status-history and inventory-movement rows are postconditions of successful commands.
- COD collection belongs to successful UC-27; cash collection belongs to UC-29.
- Return state appears in customer/staff order detail; it does not add an enquiry UC.
- Report aggregation is implementation of UC-35 through UC-37.
- Static sale/delivery/return policy text is content, not a UC.

## Optional catalogue

| ID | Primary actor | Optional use case | Why it is optional | Impact if admitted |
| --- | --- | --- | --- | --- |
| OPT-01 | Manager | Manage Automatic Promotion Campaigns | Adds price-time overlap and checkout/report tests but is not needed for sales correctness | Add promotion screens/APIs, 2 tables, BRs, price query/indexes, test matrix |
| OPT-02 | Staff/Manager | View Operational Dashboard | Derives current counts already available in fulfillment/return/inventory views | Add one screen/query set; no new business tables |

Neither optional ID is renumbered into UC-01–37 unless the team formally revises every
catalogue, diagram, description, backlog row, traceability row, and document reference.

## Safe capacity response

The two optional features are already outside commitment. If the baseline still exceeds
capacity, postpone in this order: UC-07, visual/chart polish while retaining report tables,
then UC-31. Do not remove ledgers, transaction guards, staff enquiry, fulfillment steps,
item-level return quantities, refund truth, or authorization tests.
