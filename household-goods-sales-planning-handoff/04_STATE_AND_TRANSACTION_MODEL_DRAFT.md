# State, Stock, Money, and Transaction Model

Status: **recommended logical baseline; not a physical ERD or SDS**

## Delivery order states

```text
DELIVERY + ONLINE payment:
         PENDING_PAYMENT -> CONFIRMED -> PACKING -> SHIPPING -> COMPLETED
              |                 |           |           |
              +-> CANCELLED     +-----------+           +-> DELIVERY_FAILED

DELIVERY + COD: CONFIRMED -> PACKING -> SHIPPING -> COMPLETED or DELIVERY_FAILED
COUNTER + CASH: created directly as COMPLETED by one atomic command
```

Exact transition and effect table:

| From | To | Command/trigger | Stock effect | Money effect |
| --- | --- | --- | --- | --- |
| none | PENDING_PAYMENT | Place DELIVERY order with ONLINE payment | reserve | none |
| none | CONFIRMED | Place COD order | reserve | none |
| PENDING_PAYMENT | CONFIRMED | Verified online success | none | append ONLINE collection |
| PENDING_PAYMENT | CANCELLED | Customer/Manager cancel or expiry | release reserve | refund due only if a valid collection exists/arrives |
| CONFIRMED | PACKING | Pack order | none | none |
| CONFIRMED | CANCELLED | Customer/Manager cancel | release reserve | refund due if collected |
| PACKING | SHIPPING | Dispatch | on-hand and reserve decrease | none |
| PACKING | CANCELLED | Manager cancel | release reserve | refund due if collected |
| SHIPPING | COMPLETED | Delivery success | none | append exact COD collection when COD |
| SHIPPING | DELIVERY_FAILED | Parcel physically returned after failure | on-hand increases | refund due if collected |
| none | COMPLETED | Counter cash sale | on-hand decreases | append CASH collection |

`COMPLETED`, `DELIVERY_FAILED`, and `CANCELLED` are terminal. The baseline has no
redispatch, reopen, or backward state transition. Returns never change a completed order.

## Payment attempt and money model

```text
CREATED -> PENDING -> SUCCEEDED
                   -> FAILED
                   -> EXPIRED
EXPIRED -----------^  verified late success only
```

- A retry creates a new attempt; one order has at most one open attempt.
- Payment attempt status is not proof that money exists; a verified success appends one
  immutable collection transaction.
- Provider transaction reference is unique when present.
- Duplicate verified results return the existing result and append nothing.
- A valid success after cancellation/expiry moves the matching attempt from EXPIRED to
  SUCCEEDED, records collection once, leaves the order cancelled, and creates a full
  refund-due projection.
- Collected/refunded/net amounts are sums of immutable `payment_transaction` rows.

Derived money states shown by APIs may be `UNPAID`, `PAID`, `REFUND_DUE`, or `REFUNDED`.
They are projections, not a second independently editable order status.

## Return states

```text
REQUESTED -> APPROVED -> RECEIVED -> REFUNDED
          -> REJECTED
```

- One sales order has at most one Return Request.
- Approval covers the complete requested line set; there is no partial approval.
- Receipt occurs once. Per line, `sellable + non_sellable = received <= requested`.
- `RECEIVED` means physical intake/inspection completed, not money returned.
- `REFUNDED` means the exact eligible money transaction was recorded.
- `REJECTED` and `REFUNDED` are terminal.

## Stock model

For one SKU:

```text
available_quantity = on_hand_quantity - reserved_quantity
0 <= reserved_quantity <= on_hand_quantity
```

| Event | On-hand delta | Reserved delta | Movement type |
| --- | ---: | ---: | --- |
| Create non-zero seed/opening stock | `+qty` | 0 | `OPENING_BALANCE` |
| Place delivery order | 0 | `+qty` | `ORDER_RESERVE` |
| Cancel/expire before dispatch | 0 | `-qty` | `ORDER_RELEASE` |
| Receive stock | `+qty` | 0 | `STOCK_RECEIPT` |
| Manager adjustment | signed | 0 | `ADJUSTMENT` |
| Dispatch | `-qty` | `-qty` | `DELIVERY_DISPATCH` |
| Failed parcel returned | `+qty` | 0 | `DELIVERY_RETURN` |
| Counter sale | `-qty` | 0 | `COUNTER_SALE` |
| Sellable customer return | `+qty` | 0 | `CUSTOMER_RETURN` |

Non-sellable returned quantity is captured in `return_item`; it creates no saleable stock
movement. Every movement stores after-balances so a reviewer can reconcile one event without
replaying the full ledger.

## Lock and transaction order

All row-locking commands follow one global order:

```text
customer_address when applicable
-> sales_order
-> return_request/payment_attempt when applicable
-> product_sku ascending ID
```

Commands without an order lock SKUs ascending. Provider HTTP and email calls never execute
while database locks are held.

Use PostgreSQL `READ COMMITTED` with explicit `FOR UPDATE`/pessimistic repository methods on
the rows listed below, database unique/CHECK constraints, and expected `version` for stale UI
commands. Do not raise the whole application to SERIALIZABLE or rely on an in-process Java
lock; correctness must survive concurrent requests to the database.

| Command | Locks/checks | Atomic commit |
| --- | --- | --- |
| Place Customer Order | request-key uniqueness; owned address/version; selected SKUs ascending | address snapshot, order/items/history, reservation balances/movements |
| Create payment attempt | order | open attempt with expected amount/expiry |
| Finalize provider result | order, then attempt | attempt result, collection, confirmation/history |
| Expire pending order | order, then order SKUs ascending | cancellation/history, release balances/movements, attempt expiry |
| Cancel order | order, open attempt, then order SKUs ascending | state/history, attempt expiry, release movements, refund-due projection |
| Receive stock | request-key uniqueness; SKUs ascending | receipt/items, balances, movements |
| Adjust stock | SKU and expected version | validated balance/version and movement |
| Pack | order | state/history |
| Dispatch | order, then order SKUs ascending | state/history, physical/reserved balances/movements |
| Record delivery success | order | COD collection when needed, state/history/completion time |
| Record delivery failure | order, then order SKUs ascending | returned balances/movements, state/history/failure reason |
| Counter sale | request-key uniqueness; SKUs ascending | order/items/history, cash collection, balances/movements |
| Create return request | order; return uniqueness | return header/items |
| Review return | return request | decision fields and state |
| Receive return | order, return, then affected SKUs ascending | intake/dispositions, sellable balances/movements, state |
| Record refund | order, then return when applicable | capped payment transaction and return completion when applicable |

Every rejected command rolls back fully. Status/version guards make duplicate transition
submissions harmless. Creation commands use stored request keys.

## Planned 17-table logical model

| # | Table | Primary purpose | Key invariants |
| ---: | --- | --- | --- |
| 1 | `user_account` | Customer/Staff/Manager identity and contact | normalized unique email; role/status checks; password hash only |
| 2 | `account_token` | Verification, Staff setup, and password reset | unique hash; type; expiry; one-use timestamp |
| 3 | `customer_address` | Customer-owned delivery address book | active/default rules; one active default per Customer |
| 4 | `category` | Product classification | case-insensitive unique name; active state |
| 5 | `brand` | Product classification | case-insensitive unique name; active state |
| 6 | `product` | Public content, image URL, manufacturer/warranty text | Category/Brand FKs; active state; no historical rewrite |
| 7 | `product_sku` | Variant, price, thresholds, stock balances | unique SKU code; nonnegative/reservation checks; version |
| 8 | `sales_order` | Channel, lifecycle, delivery/money snapshots | `DELIVERY`/`COUNTER`; unique scoped request key plus fingerprint; status/payment checks; totals |
| 9 | `sales_order_item` | Immutable SKU/Product/Category/price/quantity snapshots | positive quantity/amounts; unique SKU per order |
| 10 | `order_status_history` | Immutable transition evidence | order/from/to/actor/time/reason/reference |
| 11 | `payment_attempt` | Online provider attempt lifecycle | unique attempt/provider references; expected amount |
| 12 | `payment_transaction` | Immutable collection/refund ledger | positive amount; collection/refund type; source reference |
| 13 | `inventory_movement` | Immutable physical/reservation ledger | non-zero delta; after-balances; source references |
| 14 | `stock_receipt` | External delivery header | unique scoped request key plus fingerprint; receipt code; source text only |
| 15 | `stock_receipt_item` | Receipt SKU quantities | positive quantity; one SKU per receipt |
| 16 | `return_request` | One return aggregate per completed order | unique order/scoped request key plus fingerprint; request channel/reason/status; decision/intake audit |
| 17 | `return_item` | Requested/received/disposition quantities | one order item per request; quantity/disposition checks |

There is no Customer Profile, Role, Cart, Supplier, Purchase Order, Promotion, Invoice,
Refund, Delivery, generic Audit, Dashboard, or Report table in the baseline. Those concepts
are either columns/projections/ledgers, explicitly optional, or outside scope.

## Query boundaries

| Query/view | Time basis | Granularity | Stable sort |
| --- | --- | --- | --- |
| Product catalogue | current active state | Product/SKU projection | name, product ID |
| Customer order history | placed time | owned order rows | placed time desc, ID desc |
| Staff order search | placed time + current status | operational order rows | placed time desc, ID desc |
| Fulfillment queue | current order status | actionable rows | status priority, placed time, ID |
| Inventory overview | current balances | one row per SKU | product/SKU code |
| Movement history | movement time | one immutable event | occurred time desc, ID desc |
| Cash report | payment transaction time | date/method/type aggregate | local date, method |
| Sales report | order completion time | order/product aggregate | completion date/product |
| Inventory report | inventory movement time | opening/deltas/closing per SKU | SKU code |

The classroom dataset may use ordinary bounded offset pagination for explainability. Every
paged query still has a deterministic unique tie-breaker and a matching index. Deep keyset
pagination is future optimization, not baseline scope.
